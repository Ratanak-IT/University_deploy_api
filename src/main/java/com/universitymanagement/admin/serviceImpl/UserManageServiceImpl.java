package com.universitymanagement.admin.serviceImpl;

import com.universitymanagement.admin.dto.request.AdminResetPasswordRequest;
import com.universitymanagement.admin.dto.request.UpdateStatusRequest;
import com.universitymanagement.admin.dto.response.AdminDetailResponse;
import com.universitymanagement.admin.dto.response.LoginHistoryResponse;
import com.universitymanagement.admin.dto.response.UserDetailResponse;
import com.universitymanagement.admin.dto.response.UserSummaryResponse;
import com.universitymanagement.admin.entity.Admin;
import com.universitymanagement.admin.mapper.AdminUserMapper;
import com.universitymanagement.admin.repository.AdminRepository;
import com.universitymanagement.admin.service.UserManageService;
import com.universitymanagement.identity.auth.dto.request.CreateUserRequest;
import com.universitymanagement.identity.auth.dto.request.UpdateUserRequest;
import com.universitymanagement.identity.auth.dto.response.CreateUserResponse;

import com.universitymanagement.identity.auth.keycloak.client.KeycloakClient;
import com.universitymanagement.identity.entity.AccountStatus;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.exception.DuplicateResourceException;
import com.universitymanagement.identity.repository.RefreshTokenRepository;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.identity.util.RoleCodeGenerator;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.repository.StudentRepository;
import com.universitymanagement.teacher.entity.Teacher;
import com.universitymanagement.teacher.repository.TeacherRepository;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import com.universitymanagement.identity.enums.RoleName;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserManageServiceImpl implements UserManageService {
    private final Keycloak keycloak;
    private final AdminUserMapper userMapper;
    /** Keycloak's own id for "make the user pick a new password at next login". */
    private static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";

    private final KeycloakClient keycloakClient;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AdminRepository adminRepository;
    private final RoleCodeGenerator roleCodeGenerator;

    @Value("${keycloak.target-realm}")
    private String realm;

    @Override
    public Page<UserSummaryResponse> findAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        List<UserRepresentation> keycloakUsers = keycloak
                .realm(realm)
                .users()
                .list(page * size, size);

        List<UserSummaryResponse> content = keycloakUsers.stream()
                .map(kcUser -> {
                    List<String> roles = new java.util.ArrayList<>(fetchRealmRoles(kcUser.getId()));
                    if (roles.isEmpty()) {
                        userRepository.findByKeycloakId(kcUser.getId()).ifPresent(u -> {
                            if (teacherRepository.findByUserId(u.getId()).isPresent()) {
                                roles.add("TEACHER");
                            } else if (adminRepository.findByUserId(u.getId()).isPresent()) {
                                roles.add("ADMIN");
                            } else if (studentRepository.findByUserId(u.getId()).isPresent()) {
                                roles.add("STUDENT");
                            }
                        });
                    }
                    return new UserSummaryResponse(
                            kcUser.getId(),
                            kcUser.getUsername(),
                            kcUser.getEmail(),
                            kcUser.getFirstName(),
                            kcUser.getLastName(),
                            kcUser.isEnabled(),
                            roles
                    );
                })
                .toList();

        long total = keycloak
                .realm(realm)
                .users()
                .count();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public UserDetailResponse findUserById(String id) {
        UserRepresentation user = requireKeycloakUser(id);
        List<String> roles = fetchRealmRoles(id);
        return userMapper.toUserDetailResponse(user, roles);
    }

    @Override
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Password not match");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists.");
        }

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername(request.email());
        kcUser.setEmail(request.email());
        kcUser.setFirstName(request.firstName());
        kcUser.setLastName(request.lastName());
        Map<String, List<String>> attributes = new HashMap<>();

        if (request.phoneNumber() != null) {
            attributes.put("phone", List.of(request.phoneNumber()));
        }
        if (request.dateOfBirth() != null) {
            attributes.put("dateOfBirth", List.of(request.dateOfBirth().toString()));
        }
        if (request.gender() != null) {
            attributes.put("gender", List.of(request.gender().getGender()));
        }

        kcUser.setAttributes(attributes);

        kcUser.setEnabled(true);
        kcUser.setEmailVerified(true);

        // The password on a newly created account was typed by whoever created
        // it, so until the owner replaces it the registrar knows it too. Marking
        // it temporary makes Keycloak demand a new one the first time they sign
        // in, which is the only point at which the account becomes theirs alone.
        //
        // Students and teachers only. Both reach Keycloak through the browser
        // redirect flow, where it can present its own update-password screen.
        // The admin app signs in by posting credentials straight to the token
        // endpoint, and that grant has nowhere to show such a screen — Keycloak
        // refuses it outright with "Account is not fully set up", which would
        // lock a new administrator out of the very system that created them.
        boolean mustChangePassword = request.role() == RoleName.STUDENT
                || request.role() == RoleName.TEACHER;

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());
        credential.setTemporary(mustChangePassword);
        kcUser.setCredentials(List.of(credential));

        if (mustChangePassword) {
            // Set explicitly as well as through the temporary flag. Keycloak
            // derives the action from the flag today, but a later password
            // reset that clears the flag would otherwise drop the requirement
            // silently.
            kcUser.setRequiredActions(List.of(UPDATE_PASSWORD));
        }

        String keycloakId = keycloakClient.createUser(kcUser);
        keycloakClient.assignRealmRole(keycloakId, request.role().name());

        User user = new User();
        user.setId(java.util.UUID.randomUUID());
        user.setKeycloakId(keycloakId);
        user.setEmail(request.email());
        user.setDateOfBirth(request.dateOfBirth());
        user.setFullName((request.firstName() + " " + request.lastName()).trim());
        user.setPhoneNumber(request.phoneNumber());
        user.setGender(request.gender());
        user.setAccountStatus(AccountStatus.ACTIVE.name());
        user.setIsActive(true);
        User savedUser = userRepository.save(user);
        createRoleProfile(savedUser, request);

        return new CreateUserResponse(
                savedUser.getId(),
                keycloakId,
                request.email(),
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                request.dateOfBirth(),
                request.gender(),
                request.role(),
                true
        );
    }

    private void createRoleProfile(User user, CreateUserRequest request) {
        switch (request.role()) {
            case STUDENT -> {
                Student student = new Student();
                student.setUser(user);
                student.setStudentCode(roleCodeGenerator.generate("STU"));
                student.setEnrollmentDate(LocalDate.now());
                student.setAcademicYear(currentAcademicYear());
                student.setYearLevel(1);
                student.setSemester(1);
                student.setGraduationStatus("enrolled");
                studentRepository.save(student);
            }
            case TEACHER -> {
                Teacher teacher = new Teacher();
                teacher.setUser(user);
                teacher.setTeacherCode(roleCodeGenerator.generate("TCH"));
                teacher.setHireDate(LocalDate.now());
                teacher.setEmploymentStatus("active");
                teacherRepository.save(teacher);
            }
            case ADMIN -> {
                Admin admin = new Admin();
                admin.setUser(user);
                admin.setAdminCode(roleCodeGenerator.generate("ADM"));
                adminRepository.save(admin);
            }
        }
    }

    private String currentAcademicYear() {
        int currentYear = java.time.Year.now().getValue();
        return currentYear + "-" + (currentYear + 1);
    }

    @Override
    public AdminDetailResponse findAdminById(String id) {
        UserRepresentation kcUser = requireKeycloakUser(id);
        List<String> roles = fetchRealmRoles(id);

        User user = userRepository.findByKeycloakId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in local DB: " + id));

        Admin admin = adminRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin profile not found for user: " + id));

        return new AdminDetailResponse(
                kcUser.getId(),
                kcUser.getUsername(),
                kcUser.getEmail(),
                kcUser.getFirstName(),
                kcUser.getLastName(),
                Boolean.TRUE.equals(kcUser.isEnabled()),
                roles,
                admin.getAdminCode(),
                admin.getPosition(),
                admin.getDepartment()
        );
    }

    @Override
    public UserDetailResponse updateUser(String id, UpdateUserRequest request) {
        UserRepresentation kcUser = requireKeycloakUser(id);

        if (request.email() != null && !request.email().isBlank()) {
            kcUser.setEmail(request.email());
        }
        if (request.firstName() != null && !request.firstName().isBlank()) {
            kcUser.setFirstName(request.firstName());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            kcUser.setLastName(request.lastName());
        }

        keycloakClient.updateUser(kcUser);

        userRepository.findByKeycloakId(id).ifPresent(user -> {
            if (request.email() != null && !request.email().isBlank()) {
                user.setEmail(request.email());
            }
            if (request.firstName() != null || request.lastName() != null) {
                String first = request.firstName() != null ? request.firstName() : "";
                String last = request.lastName() != null ? request.lastName() : "";
                String fullName = (first + " " + last).trim();
                if (!fullName.isBlank()) {
                    user.setFullName(fullName);
                }
            }
            if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
                user.setPhoneNumber(request.phoneNumber());
            }
            userRepository.save(user);
        });

        return userMapper.toUserDetailResponse(kcUser, fetchRealmRoles(id));
    }

    @Override
    public void updateStatus(String id, UpdateStatusRequest request) {
        boolean enabled = request.status() == AccountStatus.ACTIVE;

        syncKeycloakAndLocalUser(id,
                kcUser -> kcUser.setEnabled(enabled),
                localUser -> {
                    localUser.setAccountStatus(request.status().name());
                    localUser.setIsActive(enabled);
                });
    }

    @Override
    public void resetPassword(String id, AdminResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Password not match");
        }
        requireKeycloakUser(id);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.newPassword());
        credential.setTemporary(request.temporary());

        keycloakClient.resetPassword(id, credential);
    }

    @Override
    public List<LoginHistoryResponse> getLoginHistory(String id) {
        User user = userRepository.findByKeycloakId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id));

        return refreshTokenRepository.findByUser_IdOrderByLoginTimeDesc(user.getId())
                .stream()
                .map(userMapper::toLoginHistoryResponse)
                .toList();
    }

    @Override
    public void deleteUser(String id) {
        requireKeycloakUser(id);
        keycloakClient.deleteUser(id);
        userRepository.findByKeycloakId(id).ifPresent(userRepository::delete);
    }

    @Override
    public void softDeleteUser(String id) {
        syncKeycloakAndLocalUser(id,
                kcUser -> {
                    kcUser.setEnabled(false);
                    kcUser.singleAttribute("deleted", "true");
                },
                localUser -> {
                    localUser.setIsActive(false);
                    localUser.setAccountStatus(AccountStatus.SUSPENDED.name());
                });
    }

    @Override
    public void restoreUser(String id) {
        syncKeycloakAndLocalUser(id,
                kcUser -> {
                    kcUser.setEnabled(true);
                    if (kcUser.getAttributes() != null) {
                        kcUser.getAttributes().remove("deleted");
                    }
                },
                localUser -> {
                    localUser.setIsActive(true);
                    localUser.setAccountStatus(AccountStatus.ACTIVE.name());
                });
    }


    private void syncKeycloakAndLocalUser(
            String id,
            java.util.function.Consumer<UserRepresentation> keycloakMutation,
            java.util.function.Consumer<User> localMutation) {

        UserRepresentation kcUser = requireKeycloakUser(id);
        keycloakMutation.accept(kcUser);
        keycloakClient.updateUser(kcUser);

        userRepository.findByKeycloakId(id).ifPresent(localUser -> {
            localMutation.accept(localUser);
            userRepository.save(localUser);
        });
    }

    private UserRepresentation requireKeycloakUser(String id) {
        UserRepresentation user = keycloakClient.findUserById(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id);
        }
        return user;
    }

    private List<String> fetchRealmRoles(String id) {
        try {
            return keycloak.realm(realm)
                    .users()
                    .get(id)
                    .roles()
                    .realmLevel()
                    .listAll()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .toList();
        } catch (NotFoundException e) {
            return List.of();
        }
    }

    @Override
    @Transactional
    public void assignRole(String userId, String roleName) {
        UserRepresentation user = requireKeycloakUser(userId);
        keycloakClient.assignRealmRole(user.getId(), roleName.toUpperCase());
    }
}