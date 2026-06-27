package com.solofounder.horseracing.dto.horse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHorseRequest {

    @NotBlank(message = "Horse name is required")
    @Schema(example = "Thunder")
    private String horseName;

    @NotBlank(message = "Color is required")
    @Schema(example = "Brown")
    private String color;

    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Horse age must be between 1 and 30")
    @Max(value = 30, message = "Horse age must be between 1 and 30")
    @Schema(example = "4")
    private Short age;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "M|F", message = "Horse gender must be M or F")
    @Schema(example = "M", allowableValues = {"M", "F"})
    private String gender;

    @Schema(example = "Healthy")
    private String healthNote;

    /**
     * NEW = backend auto-assigns rating (score=0, class=5), no Staff approval needed.
     * PREVIOUSLY_REGISTERED = Owner declares previous rating; Staff must verify before race registration.
     */
    @Schema(example = "NEW", allowableValues = {"NEW", "PREVIOUSLY_REGISTERED"})
    private String registrationType; // defaults to NEW if null

    /** Required when registrationType = PREVIOUSLY_REGISTERED */
    @DecimalMin(value = "0.0", message = "Claimed score must be >= 0")
    @Schema(example = "35.0")
    private BigDecimal claimedScore;

    /** Evidence link (Google Drive / OneDrive) for PREVIOUSLY_REGISTERED horses */
    @Schema(example = "https://drive.google.com/...")
    private String evidenceLink;
}
