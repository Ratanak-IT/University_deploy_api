package com.universitymanagement.sync;



import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.auth.mapper.UserMapper;
import com.universitymanagement.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeycloakSyncService {

    private final Keycloak keycloak;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Value("${keycloak.target-realm}")
    private String realm;

    public void syncUsers() {

        int first = 0;
        int size = 100;

        while (true) {

            List<UserRepresentation> users = keycloak.realm(realm)
                    .users()
                    .list(first, size);

            if (users.isEmpty()) break;

            for (UserRepresentation kcUser : users) {

                if (!userRepository.existsByKeycloakId(kcUser.getId())) {

                    User user = userMapper.toEntity(kcUser);
                    userRepository.save(user);
                }
            }

            first += size;
        }
    }
}