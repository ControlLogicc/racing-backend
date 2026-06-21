package com.solofounder.horseracing.model;

import com.solofounder.horseracing.model.converter.RaceInvitationStatusConverter;
import com.solofounder.horseracing.model.enums.RaceInvitationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "race_invitation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long invitationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private RaceRegistration raceRegistration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jockey_id", nullable = false)
    private Jockey jockey;

    @Convert(converter = RaceInvitationStatusConverter.class)
    @Column(name = "invitation_status", nullable = false, length = 20)
    private RaceInvitationStatus status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (sentAt == null) {
            sentAt = now;
        }
        if (status == null) {
            status = RaceInvitationStatus.SENT;
        }
    }
}
