package com.expmatik.backend.chat;

import java.time.LocalDateTime;

import com.expmatik.backend.maintenance.Maintenance;
import com.expmatik.backend.model.BaseEntity;
import com.expmatik.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
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
public class Chat extends BaseEntity {

    @NotBlank
    @Size(max = 1000)
    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @NotNull
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "maintenance_id", nullable = false)
    private Maintenance maintenance;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

}
