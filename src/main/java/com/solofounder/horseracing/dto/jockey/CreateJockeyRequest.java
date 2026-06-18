package com.solofounder.horseracing.dto.jockey;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateJockeyRequest {

    @NotNull(message = "User id is required")
    private Long userId;

    private BigDecimal weight;
    private Short experienceYears;
    private String status;
}
