package com.solofounder.horseracing.model;

import com.solofounder.horseracing.model.enums.HorseRegistrationType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "horse")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Horse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "horse_id")
    private Long horseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "horse_name", nullable = false, length = 100)
    private String horseName;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "age")
    private Short age;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "current_score", precision = 8, scale = 2)
    private BigDecimal currentScore;

    @Column(name = "horse_class")
    private Short horseClass;

    @Column(name = "health_Note", length = 255)
    private String healthNote;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Transient
    private Integer totalWins;

    // ── Registration type ──────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "registration_type", nullable = false, length = 25)
    private HorseRegistrationType registrationType;

    /** Score declared by Owner when type = PREVIOUSLY_REGISTERED */
    @Column(name = "claimed_score", precision = 8, scale = 2)
    private BigDecimal claimedScore;

    /** Class declared by Owner when type = PREVIOUSLY_REGISTERED (1-5) */
    @Column(name = "claimed_class")
    private Short claimedClass;

    /** true = rating accepted (auto for NEW, requires Staff approval for PREVIOUSLY_REGISTERED) */
    @Column(name = "rating_verified", nullable = false)
    private boolean ratingVerified;

    /** Staff who approved/rejected the claimed rating */
    @Column(name = "rating_verified_by")
    private Long ratingVerifiedBy;

    @Column(name = "rating_verified_at")
    private LocalDateTime ratingVerifiedAt;

    @PrePersist
    protected void onCreate() {
        if (this.currentScore == null) {
            this.currentScore = BigDecimal.ZERO;
        }
        if (this.horseClass == null) {
            this.horseClass = 5;
        }
        if (this.totalWins == null) {
            this.totalWins = 0;
        }
        if (this.status == null || this.status.isBlank()) {
            this.status = "active";
        }
        if (this.registrationType == null) {
            this.registrationType = HorseRegistrationType.NEW;
        }
    }
}
