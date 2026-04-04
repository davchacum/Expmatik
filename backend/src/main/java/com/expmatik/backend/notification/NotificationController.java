package com.expmatik.backend.notification;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expmatik.backend.notification.DTOs.NotificationCreate;
import com.expmatik.backend.notification.DTOs.NotificationResponse;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "NotificationController", description = "Controlador para gestionar las notificaciones de los usuarios.")
@Validated
public class NotificationController {

    public final NotificationService notificationService;
    public final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Integer> getUnreadNotificationCount() {
        User user = userService.getUserProfile();
        return ResponseEntity.ok((int) notificationService.countUnreadNotifications(user));
    }

    @PatchMapping("{id}/mark-as-read")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable UUID id) {
        User user = userService.getUserProfile();
        Notification notification = notificationService.markAsRead(id, user);
        return ResponseEntity.ok(NotificationResponse.fromEntity(notification));
    }

    @PatchMapping("/mark-all-as-read")
    public ResponseEntity<?> markAllNotificationsAsRead() {
        User user = userService.getUserProfile();
        notificationService.markAllAsRead(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("{id}")
    public ResponseEntity<?> getNotificationById(@PathVariable UUID id) {
        User user = userService.getUserProfile();
        return ResponseEntity.ok(NotificationResponse.fromEntity(notificationService.findById(id, user)));
    }

    @GetMapping
    public ResponseEntity<?> searchNotifications(
        @RequestParam(required = false) Boolean isRead,
        @RequestParam(required = false) NotificationType notificationType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @ParameterObject Pageable pageable
    ) {
        User user = userService.getUserProfile();
        return ResponseEntity.ok(NotificationResponse.fromEntityPage(notificationService.searchNotifications(user, isRead, notificationType, startDate, endDate, pageable)));
    }

    @PostMapping
    public ResponseEntity<?> createNotification(
        @RequestBody NotificationCreate notificationCreate
    ) {
        User user = userService.getUserProfile();
        Notification notification = notificationService.createNotification(notificationCreate.type(), notificationCreate.message(), notificationCreate.link(), user);
        return ResponseEntity.ok(NotificationResponse.fromEntity(notification));
    }
}
