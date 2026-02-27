package com.expmatik.backend.maintenance;

import java.time.LocalDate;
import java.util.List;

import com.expmatik.backend.maintenanceDetail.MaintenanceDetail;
import com.expmatik.backend.model.BaseEntity;
import com.expmatik.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.FutureOrPresent;
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
public class Maintenance extends BaseEntity{

    @NotNull
    @FutureOrPresent
    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MaintenanceStatus status;

    @NotNull
    @Size(max = 255)
    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @OneToMany
    @JoinColumn(name = "maintenance_id", nullable = false)
    private List<MaintenanceDetail> maintenanceDetails;

}
