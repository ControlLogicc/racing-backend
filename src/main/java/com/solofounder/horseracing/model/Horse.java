package com.solofounder.horseracing.model;

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

    @Column(name = "age")
    private Short age;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "breed", length = 50)
    private String breed;

    @Column(name = "current_score", precision = 8, scale = 2)
    private BigDecimal currentScore;

    @Column(name = "horse_class")
    private Short horseClass;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.currentScore == null) {
            this.currentScore = BigDecimal.ZERO;
        }
        if (this.horseClass == null) {
            this.horseClass = 5;
        }
        if (this.status == null || this.status.isBlank()) {
            this.status = "active";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
