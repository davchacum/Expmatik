package com.expmatik.backend.user;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "refresh_tokens", uniqueConstraints = {
        @UniqueConstraint(
                name="user_id_device_id",
                columnNames = {"user_id", "deviceId"}
        )
})
public class RefreshToken {

    @Id
    @SequenceGenerator(name = "rt_seq",
            sequenceName = "entity_sequence",
            initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rt_seq")
    private Integer id;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private Date expiration;

    @ManyToOne(optional = true)
    private User user;
}



