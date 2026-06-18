package com.solofounder.horseracing.dto.horse;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HorseResponse {
    private Long horseId;
    private Long ownerId;
    private String ownerName;
    private String horseName;
    private Short age;
    private String gender;
    private String breed;
    private BigDecimal currentScore;
    private Short horseClass;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
