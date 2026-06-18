package com.solofounder.horseracing.dto.jockey;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JockeyResponse {
    private Long jockeyId;
    private Long userId;
    private String fullName;
    private String email;
    private BigDecimal weight;
    private Short experienceYears;
    private String status;
    private LocalDateTime createdAt;
}
