package com.solofounder.horseracing.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "jockey")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Jockey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "jockey_id")
    private Long jockeyId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "experience_years")
    private Short experienceYears;

    @Column(name = "height", precision = 5, scale = 2)
    private BigDecimal height;

    @Column(name = "nationality", length = 100)
    private String nationality;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Column(name = "achievements", columnDefinition = "NVARCHAR(MAX)")
    private String achievements;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null || this.status.isBlank()) {
            this.status = "available";
        }
    }
}
