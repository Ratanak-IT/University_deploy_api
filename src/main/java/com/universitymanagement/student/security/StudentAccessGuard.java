package com.universitymanagement.student.security;

import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StudentAccessGuard {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User not found in local DB"));
    }
    public Student getCurrentStudent() {
        User user = getCurrentUser();
        return studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Student profile not found for current user"));
    }

    public Student requireSelfOrStaff(UUID studentId) {
        Student target = findStudent(studentId);
        if (hasRole("ADMIN") || hasRole("TEACHER")) {
            return target;
        }
        Student current = getCurrentStudent();
        if (!current.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only access your own student data");
        }
        return target;
    }

    public Student requireSelf(UUID studentId) {
        Student current = getCurrentStudent();
        if (!current.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only perform this action on your own account");
        }
        return current;
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role));
    }

    private Student findStudent(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student not found with id: " + studentId));
    }
}
