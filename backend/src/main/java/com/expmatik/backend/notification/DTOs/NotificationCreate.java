package com.expmatik.backend.notification.DTOs;

import com.expmatik.backend.notification.NotificationType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationCreate(

    @NotNull
    @Size(max = 500)
    String message,

    @NotNull
    @Enumerated(EnumType.STRING)
    NotificationType type,

    @NotNull
    @Size(max = 255)
    String link
) {

}
