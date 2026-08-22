# Server setup and domain

What to do once, by hand, before CI/CD can take over. After this, pushing to
`main` is the whole deploy.

```
Internet ──▶ :443  Nginx Proxy Manager ──┐  (TLS terminates here)
                                          │  university-net
                                          ├─▶ university-api:8081
                                          ├─▶ university-keycloak:8080
                                          └─▶ university-minio:9000
```

Only the proxy has published ports. Everything else is reachable by container
name on the shared network and never directly from the internet.

---

## 1. Prepare the server

Docker Engine and the Compose plugin (Ubuntu):

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER"   # log out and back in for this to take effect
docker compose version            # should print v2.x
```

Firewall — only the proxy's ports and SSH need to be open:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 81/tcp             # proxy admin UI; close it again once set up
sudo ufw enable
```

Postgres is published on `1688` and MinIO on `9000/9001` by the compose file.
If those do not need to be reachable from outside the box, do not open them in
the firewall — the containers still reach each other over `university-net`.

## 2. Put the stack on the server

```bash
mkdir -p ~/keycloak-infra && cd ~/keycloak-infra
# copy docker-compose.yml and init-scripts/ from this repo, then:
nano .env
```

`.env` sits next to `docker-compose.yml`, is read by Compose, and is never
committed:

```dotenv
POSTGRES_USER=ratanak
POSTGRES_PASSWORD=<strong password>
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=<strong password>
KEYCLOAK_CLIENT=<the university-api client secret from Keycloak>
KC_HOSTNAME=auth.careerpatch.site
KC_HOSTNAME_PORT=443
MINIO_ROOT_USER=<access key>
MINIO_ROOT_PASSWORD=<secret key>
APP_CORS_ALLOWED_ORIGINS=https://admin.careerpatch.site
```

Check the values landed where you expect before starting anything:

```bash
docker compose config | grep -A 25 university-api
```

The path you used here (`~/keycloak-infra`) is what goes into the
`SERVER_COMPOSE_PATH` GitHub secret.

## 3. Log in to GHCR and start

```bash
echo "<read:packages token>" | docker login ghcr.io -u <github-username> --password-stdin
docker compose up -d
docker compose ps
```

`university-api` should reach `healthy` within about two minutes — the first
boot runs schema updates and the one-time migrations.

## 4. Point the domain at the server

At your DNS provider, an **A record** per hostname, all to the server's IP:

| Type | Name | Value |
|---|---|---|
| A | `api` | `<server IP>` |
| A | `auth` | `<server IP>` |
| A | `s3` | `<server IP>` |
| A | `storage` | `<server IP>` |

Wait for it to resolve before asking for a certificate — Let's Encrypt
validates over HTTP, and it fails if DNS has not propagated:

```bash
dig +short api.careerpatch.site
```

## 5. Add the proxy host

Nginx Proxy Manager is already running. Open `http://<server IP>:81` (default
login `admin@example.com` / `changeme` — change it immediately).

**Hosts → Proxy Hosts → Add Proxy Host**

*Details* tab:

| Field | Value |
|---|---|
| Domain Names | `api.careerpatch.site` |
| Scheme | `http` |
| Forward Hostname / IP | `university-api` |
| Forward Port | `8081` |
| Block Common Exploits | on |
| Websockets Support | on |

The forward hostname is the **container name**, not an IP — the proxy resolves
it over `university-net`, which is why the API needs no published port.

*SSL* tab:

| Field | Value |
|---|---|
| SSL Certificate | Request a new SSL Certificate |
| Force SSL | on |
| HTTP/2 Support | on |
| HSTS Enabled | on |
| Agree to Let's Encrypt Terms | on |

Save. Then check from your own machine:

```bash
curl -s https://api.careerpatch.site/actuator/health   # {"status":"UP"}
```

Close port 81 once the proxy is configured:

```bash
sudo ufw delete allow 81/tcp
```

## 6. Point the front end at it

In the admin app's `.env`:

```dotenv
NEXT_PUBLIC_API_BASE_URL=https://api.careerpatch.site
```

The admin app proxies API calls through its own server (`/backend/...`), so the
browser only ever talks to the front-end origin and CORS does not apply. If you
ever set `NEXT_PUBLIC_API_PROXY=false`, the browser calls the API directly and
`APP_CORS_ALLOWED_ORIGINS` in the server's `.env` must list the front-end
origin — otherwise every request is blocked.

## 7. Keycloak redirect URIs

In the Keycloak admin console → realm `university` → client `university-api`,
the redirect URIs must include the deployed front end, not just localhost.
Missing this is the usual reason login works locally and fails in production.

---

## After this

Push to `main`. The workflow builds, tests, pushes the image, SSHes in, pulls,
restarts `university-api`, and waits for it to report healthy. See
[DEPLOYMENT.md](./DEPLOYMENT.md) for the secrets it needs.

## Troubleshooting

**`docker compose pull` says unauthorized** — the server's GHCR login expired or
the token lacks `read:packages`. Log in again as in step 3.

**Proxy shows 502** — the API is not healthy. `docker compose logs --tail=100
university-api`. Usually a wrong value in `.env`, most often the database
password or the Keycloak client secret.

**Certificate request fails** — DNS has not propagated, or ports 80/443 are not
reachable. Check `dig +short api.careerpatch.site` and the firewall.

**Login works locally, fails in production** — Keycloak redirect URIs (step 7),
or `KC_HOSTNAME` in `.env` not matching the real domain.
