package com.solofounder.horseracing.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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

    @PrePersist
    protected void onCreate() {
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
}
