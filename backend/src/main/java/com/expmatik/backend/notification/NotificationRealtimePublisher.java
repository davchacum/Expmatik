package com.expmatik.backend.notification;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.expmatik.backend.user.User;

@Component
public class NotificationRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    public NotificationRealtimePublisher(SimpMessagingTemplate messagingTemplate, NotificationRepository notificationRepository) {
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
    }

    public void publishUnreadCount(User user) {
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
        messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications/unread-count", unreadCount);
    }
}