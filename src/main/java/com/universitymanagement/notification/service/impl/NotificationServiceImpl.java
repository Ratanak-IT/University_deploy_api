package com.universitymanagement.notification.service.impl;

import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.exception.UserNotFoundException;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.notification.dto.response.NotificationResponse;
import com.universitymanagement.notification.entity.Notification;
import com.universitymanagement.notification.repository.NotificationRepository;
import com.universitymanagement.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    public List<NotificationResponse> getMyNotifications() {
        User user = getCurrentUser();

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        User user = getCurrentUser();
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Notification not found: " + notificationId));

        if (!n.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This notification belongs to another user");
        }

        n.setRead(true);
        return toResponse(notificationRepository.save(n));
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
    public void createNotification(UUID userId, String title, String message,
                                   String type, String context, String actor) {
        createNotification(userId, title, message, type, context, actor, null, null, null);
    }

    @Override
    @Transactional
    public void createNotification(UUID userId, String title, String message,
                                   String type, String context, String actor,
                                   String link, String resourceType, UUID resourceId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setContext(context);
        n.setActor(actor);
        n.setLink(link);
        n.setResourceType(resourceType);
        n.setResourceId(resourceId);
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
                n.getLink(),
                n.getResourceType(),
                n.getResourceId(),
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
}
