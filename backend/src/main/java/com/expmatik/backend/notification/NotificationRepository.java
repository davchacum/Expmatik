package com.expmatik.backend.notification;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    @Query("""
        SELECT n FROM Notification n
        WHERE n.user.id = :userId
        AND (:isRead IS NULL OR n.isRead = :isRead)
        AND (:notificationType IS NULL OR n.notificationType = :notificationType)
        AND (n.createdAt >= COALESCE(:startDate, n.createdAt))
        AND (n.createdAt <= COALESCE(:endDate, n.createdAt))
    """)
    Page<Notification> searchNotifications(
        @Param("userId") UUID userId,
        @Param("isRead") Boolean isRead,
        @Param("notificationType") NotificationType notificationType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    long countByUserIdAndIsReadFalse(UUID userId);
}
