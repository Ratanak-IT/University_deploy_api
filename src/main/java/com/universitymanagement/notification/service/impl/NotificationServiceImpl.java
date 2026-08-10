package com.universitymanagement.notification.service.impl;

import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.exception.UserNotFoundException;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.notification.dto.response.NotificationResponse;
import com.universitymanagement.notification.entity.Notification;
import com.universitymanagement.notification.repository.NotificationRepository;
import com.universitymanagement.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public List<NotificationResponse> getMyNotifications() {
        User user = getCurrentUser();
        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        if (list.isEmpty()) {
            seedInitialNotifications(user);
            list = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        }

        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        User user = getCurrentUser();
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!n.getUserId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        n.setRead(true);
        Notification saved = notificationRepository.save(n);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        User user = getCurrentUser();
        notificationRepository.markAllAsReadByUserId(user.getId());
    }

    @Override
    public long getUnreadCount() {
        User user = getCurrentUser();
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Override
    @Transactional
    public void createNotification(UUID userId, String title, String message, String type, String context, String actor) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setContext(context);
        n.setActor(actor);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.getContext(),
                n.getActor(),
                n.isRead(),
                n.getCreatedAt()
        );
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new UserNotFoundException();
        }
        return userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(UserNotFoundException::new);
    }

    private void seedInitialNotifications(User user) {
        createNotification(
                user.getId(),
                "Grade Release",
                "Teacher released your exam scores for Web Development II (Midterm: 89/100, Final: 100/100)",
                "GRADE",
                "Web Development II (CS202-A)",
                "K. Sopheap"
        );
        createNotification(
                user.getId(),
                "Certificate Request Approved",
                "Your Certificate of Enrollment request (Serial: UMT-89412) has been APPROVED by Academic Registrar",
                "CERTIFICATE",
                "Official Academic Certificate",
                "Academic Registrar"
        );
        createNotification(
                user.getId(),
                "New Quiz Assigned",
                "Assigned a new Quiz: 'Web Security & Component Architecture'",
                "ASSIGNMENT",
                "Project Management II",
                "K. Sopheap"
        );
        createNotification(
                user.getId(),
                "Campus Announcement",
                "Semester 2 Final Examination schedule has been officially published on student portal",
                "ANNOUNCEMENT",
                "University Announcement",
                "University Admin"
        );
        createNotification(
                user.getId(),
                "Attendance Recorded",
                "Recorded Present for Web Development II class session",
                "ATTENDANCE",
                "Classroom CS202-A",
                "Attendance System"
        );
    }
}
