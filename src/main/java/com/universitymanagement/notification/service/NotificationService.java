package com.universitymanagement.notification.service;

import com.universitymanagement.notification.dto.response.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    List<NotificationResponse> getMyNotifications();

    NotificationResponse markAsRead(UUID notificationId);

    void markAllAsRead();

    long getUnreadCount();

    void createNotification(UUID userId, String title, String message, String type, String context, String actor);
}
