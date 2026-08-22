# Deployment

Push to `main` → tests run → image builds and pushes to GHCR → the server pulls
it and restarts the API → the run waits until the API reports healthy.

If any step fails, the ones after it do not run, and the workflow is marked
failed. A green run means the API answered `/actuator/health` with `UP`.

```
push to main
   │
   ├─ test ............. unit tests; a failure stops everything here
   ├─ build-and-push ... Docker image → ghcr.io/ratanak-it/university_deploy_api
   └─ deploy ........... ssh → docker compose pull && up -d → poll health
```

Pull requests run the first two steps but never push an image or touch the
server.

---

## One-time setup

### 1. The repository

CI must run in **`Ratanak-IT/University_deploy_api`**. The image name is derived
from the repository, and `keycloak-infra/docker-compose.yml` expects exactly:

```
ghcr.io/ratanak-it/university_deploy_api:latest
```

Running the same workflow in a differently-named repository produces a
differently-named image. Nothing errors — the push succeeds, and the server goes
on running the old build indefinitely. If you move the repository, change the
`image:` line in the compose file to match.

### 2. A deploy key for the server

On your machine, make a key pair that exists only for this:

```bash
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ./deploy_key -N ""
```

Put the **public** half on the server:

```bash
ssh-copy-id -i ./deploy_key.pub USER@SERVER_HOST
# or: cat deploy_key.pub >> ~/.ssh/authorized_keys
```

The **private** half goes into GitHub as a secret (below). Do not commit either.

### 3. A read-only GHCR token

The server needs to pull from GHCR. Create a classic personal access token with
only **`read:packages`**, at
<https://github.com/settings/tokens>.

Do not reuse a token with write scopes: this one lives on the server, and a
read-only token cannot be used to publish a poisoned image.

### 4. GitHub secrets

Repository → **Settings → Secrets and variables → Actions**:

| Secret | What it is | Example |
|---|---|---|
| `SERVER_HOST` | Server address | `15.235.209.49` |
| `SERVER_USER` | SSH user | `ubuntu` |
| `SERVER_SSH_KEY` | Contents of `deploy_key` — the whole file, `BEGIN`/`END` lines included | |
| `SERVER_PORT` | SSH port. Omit for 22 | `22` |
| `SERVER_COMPOSE_PATH` | Directory holding `docker-compose.yml` on the server | `/home/ubuntu/keycloak-infra` |
| `GHCR_TOKEN` | The `read:packages` token from step 3 | |

The `deploy` job uses the `production` environment. Add it under
**Settings → Environments** if you want a manual approval before anything
reaches the server — useful once real students are using it.

### 5. The server's `.env`

Next to `docker-compose.yml` on the server. Compose reads it; it is never
committed:

```dotenv
POSTGRES_USER=ratanak
POSTGRES_PASSWORD=...
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=...
KEYCLOAK_CLIENT=...            # the university-api client secret
KC_HOSTNAME=auth.careerpatch.site
KC_HOSTNAME_PORT=443
MINIO_ROOT_USER=...
MINIO_ROOT_PASSWORD=...
```

Check it before the first deploy — a missing value here is the most common
cause of a container that starts and then dies:

```bash
cd /path/to/keycloak-infra
docker compose config | grep -A 20 university-api
```

---

## Verifying a deploy

The workflow already waits for health, so a green run is the answer. To check by
hand:

```bash
curl -s https://api.careerpatch.site/actuator/health     # {"status":"UP"}
docker compose ps university-api                          # STATUS: healthy
docker compose logs --tail=100 university-api
```

## Rolling back

Images are tagged with the short commit SHA as well as `latest`, so a bad deploy
does not have to be fixed forward:

```bash
cd /path/to/keycloak-infra
docker compose pull university-api                        # or pin the tag:
docker run --rm ghcr.io/ratanak-it/university_deploy_api:abc1234 --version
```

To pin, set the tag in `docker-compose.yml` to the known-good SHA and
`docker compose up -d --no-deps university-api`.

---

## Notes on what the first boot does

The app runs `ddl-auto: update` plus two one-time migrations
(`LegacyExamScoreMigration`, `LegacyAttendanceMigration`, `QuizClassroomMigration`).
On a database with existing data the first start after this release is therefore
slower than usual — hence the 120-second `start_period` on the healthcheck.
Both migrations are guarded and do nothing on subsequent starts.

## Known gaps

- **`UniversityManagementApplicationTests.contextLoads` is not run in CI.** It
  needs a live PostgreSQL, which the runner does not have. Adding a `services:
  postgres` block to the test job would let it run and would catch broken bean
  wiring before deploy — worth doing.
- **No database backup runs before deploy.** `ddl-auto: update` will not drop a
  column, but it does alter schema on start. A `pg_dump` step before the restart
  would make a bad release recoverable.
- **No smoke test beyond health.** Health says Spring started and the datasource
  answered; it does not prove login or grading still work.
