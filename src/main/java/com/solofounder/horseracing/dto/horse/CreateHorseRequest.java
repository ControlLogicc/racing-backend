package com.solofounder.horseracing.dto.horse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    private String breed;
    @Size(max = 1000, message = "Pedigree must be at most 1000 characters")
    private String pedigree;
    private String trainerName;
    private String stableName;
    @Size(max = 2048, message = "Image URL must be at most 2048 characters")
    private String imageUrl;
    private LocalDate dateOfBirth;

    @Schema(example = "Healthy")
    @Size(max = 2048, message = "Health note must be at most 2048 characters")
    private String healthNote;

    /**
     * NEW = backend auto-assigns rating (score=50, class=3), no Staff approval needed.
     * PREVIOUSLY_REGISTERED = Owner declares previous rating; Staff must verify before race registration.
     */
    @Schema(example = "NEW", allowableValues = {"NEW", "PREVIOUSLY_REGISTERED"})
    private String registrationType; // defaults to NEW if null

    /** Required when registrationType = PREVIOUSLY_REGISTERED */
    @DecimalMin(value = "0.0", message = "Claimed score must be >= 0")
    @Schema(example = "35.0")
    private BigDecimal claimedScore;

    @Min(value = 1, message = "Claimed class must be between 1 and 5")
    @Max(value = 5, message = "Claimed class must be between 1 and 5")
    private Short claimedClass;

    /** Evidence link (Google Drive / OneDrive) for PREVIOUSLY_REGISTERED horses */
    @Size(max = 2048, message = "Evidence link must be at most 2048 characters")
    @Schema(example = "https://drive.google.com/...")
    private String evidenceLink;
}
