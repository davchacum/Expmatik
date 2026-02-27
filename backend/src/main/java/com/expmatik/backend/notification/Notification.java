package com.expmatik.backend.notification;

import java.time.LocalDateTime;
import java.util.UUID;

import com.expmatik.backend.model.BaseEntity;
import com.expmatik.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Notification extends BaseEntity {

    @NotNull
    @Size(max = 255)
    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @NotNull
    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @NotNull
    @Size(max = 255)
    @Column(name = "entity_reference", nullable = false, length = 255)
    private String entityReference;

    @NotNull
    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}
