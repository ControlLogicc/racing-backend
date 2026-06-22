package com.solofounder.horseracing.dto.horse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HorseResponse {
    private Long horseId;
    private Long ownerId;
    private String ownerName;
    private String horseName;
    private String color;
    private Short age;
    private String gender;

    @Schema(example = "0")
    private BigDecimal currentScore;

    @Schema(example = "5", minimum = "1", maximum = "5")
    private Short horseClass;

    private Integer totalWins;

    private String healthNote;
    private String status;
}
