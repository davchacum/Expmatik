package com.expmatik.backend.notification;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.user.User;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification save(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Notification findById(UUID id,User user) {
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        checkUserAuthorization(notification, user);
        return notification;
    }

    private void checkUserAuthorization(Notification notification, User user) {
        if(notification.getUser().getId() != user.getId()) {
            throw new AccessDeniedException("You are not authorized to perform this action.");
        }
    }

    @Transactional
    public Notification createNotification(NotificationType type,String message, String link, User user) {
        Notification notification = new Notification();
        notification.setCreatedAt(LocalDateTime.now());
        notification.setMessage(message);
        notification.setType(type);
        notification.setLink(link);
        notification.setIsRead(false);
        notification.setUser(user);
        return save(notification);
    }

    @Transactional
    public Notification markAsRead(UUID id, User user) {
        Notification notification = findById(id, user);
        notification.setIsRead(true);
        return save(notification);
    }

    @Transactional(readOnly = true)
    public Page<Notification> searchNotifications(User user, Boolean isRead, NotificationType notificationType, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return notificationRepository.searchNotifications(user.getId(), isRead, notificationType, startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnreadNotifications(User user) {
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public void markAllAsRead(User user) {
        Page<Notification> unreadNotifications = searchNotifications(user, false, null, null, null, Pageable.unpaged());
        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            save(notification);
        });
    }



}
