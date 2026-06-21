package com.solofounder.horseracing.model;

import com.solofounder.horseracing.model.enums.RaceInvitationStatus;
import com.solofounder.horseracing.model.converter.RaceInvitationStatusConverter;
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

    @Column(name = "invitation_status", nullable = false, length = 20)
    @Convert(converter = RaceInvitationStatusConverter.class)
    private RaceInvitationStatus invitationStatus;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        if (this.sentAt == null) {
            this.sentAt = now;
        }
        if (this.invitationStatus == null) {
            this.invitationStatus = RaceInvitationStatus.SENT;
        }
    }
}
