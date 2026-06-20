package com.solofounder.horseracing.model;

import com.solofounder.horseracing.model.converter.RefereeStatusConverter;
import com.solofounder.horseracing.model.enums.RefereeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "referee")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Referee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "referee_id")
    private Long refereeId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "license_no", nullable = false, unique = true, length = 40)
    private String licenseNo;

    @Convert(converter = RefereeStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private RefereeStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
