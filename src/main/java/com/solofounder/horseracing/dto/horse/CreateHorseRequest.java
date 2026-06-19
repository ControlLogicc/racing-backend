package com.solofounder.horseracing.dto.horse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

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
}
