package com.solofounder.horseracing.model;

import com.solofounder.horseracing.model.converter.RefereeReportTypeConverter;
import com.solofounder.horseracing.model.enums.RefereeReportType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "referee_report")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefereeReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_id", nullable = false)
    private Referee referee;

    @Column(name = "report_type", nullable = false, length = 25)
    @Convert(converter = RefereeReportTypeConverter.class)
    private RefereeReportType reportType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "decision", length = 25)
    private String decision;

    @Column(name = "penalty", length = 255)
    private String penalty;

    @Column(name = "report_status", nullable = false, length = 20)
    private String reportStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.reportStatus == null) {
            this.reportStatus = "submitted";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
