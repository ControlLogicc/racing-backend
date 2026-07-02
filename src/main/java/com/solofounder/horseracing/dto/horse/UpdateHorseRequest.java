package com.solofounder.horseracing.dto.horse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHorseRequest {

    @NotBlank(message = "Horse name is required")
    @Schema(example = "Thunder Updated")
    private String horseName;

    @NotBlank(message = "Color is required")
    @Schema(example = "Black")
    private String color;

    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Horse age must be between 1 and 30")
    @Max(value = 30, message = "Horse age must be between 1 and 30")
    @Schema(example = "5")
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

    @NotBlank(message = "Status is required")
    @Schema(example = "ACTIVE", allowableValues = {"ACTIVE", "INJURED", "RETIRED", "SUSPENDED"})
    private String status;
}
