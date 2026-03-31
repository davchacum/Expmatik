package com.expmatik.backend.notification.DTOs;

import java.time.LocalDateTime;
import java.util.UUID;

import com.expmatik.backend.notification.Notification;
import com.expmatik.backend.notification.NotificationType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationResponse(

    @NotNull
    @Size(max = 500)
    String message,

    @NotNull
    LocalDateTime createdAt,

    @NotNull
    NotificationType type,

    @NotNull
    @Size(max = 255)
    String link,

    @NotNull
    Boolean isRead,

    @NotNull
    UUID userId
) {

    public static NotificationResponse fromEntity(Notification notification) {
        return new NotificationResponse(
            notification.getMessage(),
            notification.getCreatedAt(),
            notification.getType(),
            notification.getLink(),
            notification.getIsRead(),
            notification.getUser().getId()
        );
    }

}
