package com.expmatik.backend.notification.DTOs;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.expmatik.backend.notification.Notification;
import com.expmatik.backend.notification.NotificationType;

public record NotificationResponse(

    UUID id,

    String message,

    LocalDateTime createdAt,

    NotificationType type,

    String link,

    Boolean isRead,

    UUID userId
) {

    public static NotificationResponse fromEntity(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getMessage(),
            notification.getCreatedAt(),
            notification.getType(),
            notification.getLink(),
            notification.getIsRead(),
            notification.getUser().getId()
        );
    }

    public static Page<NotificationResponse> fromEntityPage(Page<Notification> notificationPage) {
        return notificationPage.map(NotificationResponse::fromEntity);
    }

}
