package com.solofounder.horseracing.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "race_entry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_id")
    private Long entryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private RaceRegistration registration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id")
    private RaceInvitation invitation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horse_id", nullable = false)
    private Horse horse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jockey_id", nullable = false)
    private Jockey jockey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_staff_id")
    private Staff confirmedByStaff;

    @Column(name = "gate_number")
    private Short gateNumber;

    @Column(name = "draw_number")
    private Short drawNumber;

    @Column(name = "handicap_weight", precision = 5, scale = 2)
    private BigDecimal handicapWeight;

    @Column(name = "actual_weight", precision = 5, scale = 2)
    private BigDecimal actualWeight;

    @Column(name = "weight_check_status", length = 25)
    private String weightCheckStatus;

    @Column(name = "pre_check_note", length = 500)
    private String preCheckNote;

    @Column(name = "entry_status", nullable = false, length = 20)
    private String entryStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.entryStatus == null) {
            this.entryStatus = "DECLARED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
