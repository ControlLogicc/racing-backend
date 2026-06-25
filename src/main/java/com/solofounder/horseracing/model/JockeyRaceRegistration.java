package com.solofounder.horseracing.model;

import com.solofounder.horseracing.model.converter.JockeyRaceRegistrationStatusConverter;
import com.solofounder.horseracing.model.enums.JockeyRaceRegistrationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "jockey_race_registration", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JockeyRaceRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "jockey_race_registration_id")
    private Long jockeyRaceRegistrationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jockey_id", nullable = false)
    private Jockey jockey;

    @Convert(converter = JockeyRaceRegistrationStatusConverter.class)
    @Column(name = "status", nullable = false, length = 30)
    private JockeyRaceRegistrationStatus status;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) {
            status = JockeyRaceRegistrationStatus.REGISTERED;
        }
        if (registeredAt == null) {
            registeredAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
