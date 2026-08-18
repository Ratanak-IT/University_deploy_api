package com.universitymanagement.identity.service;

import com.universitymanagement.identity.auth.keycloak.client.KeycloakClient;
import com.universitymanagement.identity.dto.PersonalInfoPatch;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileWriter {

    private final UserRepository userRepository;
    private final KeycloakClient keycloakClient;

    public User apply(User user, PersonalInfoPatch patch) {
        if (user == null || patch == null) {
            return user;
        }

        boolean identityChanged = false;

        if (isSet(patch.firstName())) {
            user.setFirstName(patch.firstName().trim());
            identityChanged = true;
        }
        if (isSet(patch.lastName())) {
            user.setLastName(patch.lastName().trim());
            identityChanged = true;
        }
        if (identityChanged) {
            user.syncFullName();
        } else if (isSet(patch.fullName())) {
            user.setFullName(patch.fullName().trim());
            identityChanged = true;
        }

        if (isSet(patch.nameKhmer())) {
            user.setNameKhmer(patch.nameKhmer().trim());
        }
        if (isSet(patch.email())) {
            String email = patch.email().trim().toLowerCase();
            if (!email.equalsIgnoreCase(user.getEmail())) {
                if (userRepository.existsByEmail(email)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Email already in use: " + email);
                }
                user.setEmail(email);
                identityChanged = true;
            }
        }
        if (patch.gender() != null) {
            user.setGender(patch.gender());
        }
        if (patch.dateOfBirth() != null) {
            user.setDateOfBirth(patch.dateOfBirth());
        }
        if (isSet(patch.phoneNumber())) {
            user.setPhoneNumber(patch.phoneNumber().trim());
        }
        if (isSet(patch.idCardNumber())) {
            user.setIdCardNumber(patch.idCardNumber().trim());
        }
        if (isSet(patch.placeOfBirth())) {
            user.setPlaceOfBirth(patch.placeOfBirth().trim());
        }
        if (isSet(patch.currentAddress())) {
            user.setCurrentAddress(patch.currentAddress().trim());
        }
        if (isSet(patch.address())) {
            user.setAddress(patch.address().trim());
        }
        if (isSet(patch.fatherContact())) {
            user.setFatherContact(patch.fatherContact().trim());
        }
        if (isSet(patch.motherContact())) {
            user.setMotherContact(patch.motherContact().trim());
        }

        User saved = userRepository.save(user);

        if (identityChanged || patch.phoneNumber() != null || patch.dateOfBirth() != null
                || patch.gender() != null || patch.nameKhmer() != null) {
            syncKeycloak(saved);
        }
        return saved;
    }


    private void syncKeycloak(User user) {
        if (user.getKeycloakId() == null) {
            return;
        }
        try {
            UserRepresentation kcUser = keycloakClient.findUserById(user.getKeycloakId());
            if (kcUser == null) {
                log.warn("Keycloak user {} not found — skipping sync", user.getKeycloakId());
                return;
            }

            kcUser.setFirstName(user.resolvedFirstName());
            kcUser.setLastName(user.resolvedLastName());
            kcUser.setEmail(user.getEmail());
            kcUser.setUsername(user.getEmail());

            Map<String, List<String>> attributes = kcUser.getAttributes() == null
                    ? new HashMap<>()
                    : new HashMap<>(kcUser.getAttributes());

            putAttribute(attributes, "phone", user.getPhoneNumber());
            putAttribute(attributes, "nameKhmer", user.getNameKhmer());
            putAttribute(attributes, "idCardNumber", user.getIdCardNumber());
            putAttribute(attributes, "placeOfBirth", user.getPlaceOfBirth());
            putAttribute(attributes, "currentAddress", user.getCurrentAddress());
            putAttribute(attributes, "dateOfBirth",
                    user.getDateOfBirth() == null ? null : user.getDateOfBirth().toString());
            putAttribute(attributes, "gender",
                    user.getGender() == null ? null : user.getGender().getGender());

            kcUser.setAttributes(attributes);
            keycloakClient.updateUser(kcUser);
        } catch (Exception e) {
            log.warn("Keycloak sync failed for user {}: {}", user.getKeycloakId(), e.getMessage());
        }
    }

    private void putAttribute(Map<String, List<String>> attributes, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        attributes.put(key, new ArrayList<>(List.of(value)));
    }

    private boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}