package com.solofounder.horseracing.dto.horse;

import io.swagger.v3.oas.annotations.media.Schema;
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

    // Registration type & rating verification
    @Schema(example = "NEW", allowableValues = {"NEW", "PREVIOUSLY_REGISTERED"})
    private String registrationType;

    private BigDecimal claimedScore;
    private Short claimedClass;

    @Schema(description = "true = rating verified (auto for NEW, requires Staff for PREVIOUSLY_REGISTERED)")
    private boolean ratingVerified;

    private LocalDateTime ratingVerifiedAt;
}
