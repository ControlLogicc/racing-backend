package com.solofounder.horseracing.model;

import com.solofounder.horseracing.model.enums.RaceStatus;
import com.solofounder.horseracing.model.converter.RaceStatusConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "race")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "race_id")
    private Long raceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private RaceMeeting raceMeeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id", nullable = false)
    private RaceCondition raceCondition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_id")
    private Referee referee;

    @Column(name = "race_name", nullable = false, length = 120)
    private String raceName;

    @Column(name = "race_no")
    private Short raceNo;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column(name = "registration_open_at")
    private LocalDateTime registrationOpenAt;

    @Column(name = "registration_close_at")
    private LocalDateTime registrationCloseAt;

    @Column(name = "status", nullable = false, length = 25)
    @Convert(converter = RaceStatusConverter.class)
    private RaceStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = RaceStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
