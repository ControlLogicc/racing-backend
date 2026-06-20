package com.solofounder.horseracing.dto.referee;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRefereeRequest {

    @NotBlank(message = "License number is required")
    private String licenseNo;

    @NotBlank(message = "Status is required")
    private String status;
}
