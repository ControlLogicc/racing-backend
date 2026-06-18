package com.solofounder.horseracing.dto.horse;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HorseRequest {

    @NotBlank(message = "Horse name is required")
    private String horseName;

    private Short age;
    private String gender;
    private String breed;
    private BigDecimal currentScore;
    private Short horseClass;
    private String status;
}
