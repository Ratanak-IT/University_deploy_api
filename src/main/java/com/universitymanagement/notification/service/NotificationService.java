package com.universitymanagement.notification.service;

import com.universitymanagement.notification.dto.response.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    List<NotificationResponse> getMyNotifications();

    NotificationResponse markAsRead(UUID notificationId);

    void markAllAsRead();

    long getUnreadCount();

    void createNotification(UUID userId, String title, String message,
                            String type, String context, String actor);

    void createNotification(UUID userId, String title, String message,
                            String type, String context, String actor,
                            String link, String resourceType, UUID resourceId);

    /**
     * Sends the same notification to every administrator.
     *
     * <p>Addressed by role rather than to a named person: an administrator who
     * leaves should not take the registry's alerts with them, and a new one
     * should start receiving them without anybody remembering to rewire this.
     *
     * <p>Never throws. A notification is a side effect of the thing that just
     * happened, and losing one is a smaller failure than rolling back the
     * certificate request or the grade that caused it.
     */
    void notifyAdmins(String title, String message, String type,
                      String context, String actor, String link,
                      String resourceType, UUID resourceId);
}
