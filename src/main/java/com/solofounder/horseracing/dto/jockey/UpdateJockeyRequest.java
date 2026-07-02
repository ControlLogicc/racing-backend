package com.solofounder.horseracing.dto.jockey;

import lombok.*;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateJockeyRequest {
    private BigDecimal weight;
    private Short experienceYears;
    @Positive(message = "Height must be greater than 0")
    private BigDecimal height;
    private String nationality;
    private String licenseNumber;
    private String achievements;
    @Size(max = 2048, message = "Image URL must be at most 2048 characters")
    private String imageUrl;
    private LocalDate dateOfBirth;
    private String status;
}
