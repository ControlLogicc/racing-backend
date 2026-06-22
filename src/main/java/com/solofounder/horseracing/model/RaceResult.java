package com.solofounder.horseracing.model;

import com.solofounder.horseracing.model.enums.RaceResultStatus;
import com.solofounder.horseracing.model.converter.RaceResultStatusConverter;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "race_result")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private RaceEntry entry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @Column(name = "position", nullable = false)
    private Short position;

    @Column(name = "finish_time")
    private LocalTime finishTime;

    @Column(name = "result_status", nullable = false, length = 20)
    @Convert(converter = RaceResultStatusConverter.class)
    private RaceResultStatus resultStatus;

    @Column(name = "score_awarded", precision = 6, scale = 2)
    private BigDecimal scoreAwarded;

    @Column(name = "prize_amount", precision = 12, scale = 2)
    private BigDecimal prizeAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.resultStatus == null) {
            this.resultStatus = RaceResultStatus.PROVISIONAL;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
