package com.solofounder.horseracing.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "race_condition")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "condition_id")
    private Long conditionId;

    @Column(name = "condition_name", nullable = false, length = 80)
    private String conditionName;

    @Column(name = "distance", nullable = false)
    private Integer distance;

    @Column(name = "track_type", length = 20)
    private String trackType;

    @Column(name = "min_entries")
    private Short minEntries;

    @Column(name = "max_entries")
    private Short maxEntries;

    @Column(name = "class_requirement", length = 30)
    private String classRequirement;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.minEntries == null) {
            this.minEntries = 8;
        }
        if (this.maxEntries == null) {
            this.maxEntries = 14;
        }
    }
}
