package com.solofounder.horseracing.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "racecourse")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Racecourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "racecourse_id")
    private Long racecourseId;

    @Column(name = "racecourse_name", nullable = false, length = 120)
    private String racecourseName;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "surface_type", length = 20)
    private String surfaceType;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
