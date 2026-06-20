package com.solofounder.horseracing.dto.staff;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStaffRequest {

    @NotBlank(message = "Staff code is required")
    private String staffCode;

    @NotBlank(message = "Department is required")
    private String department;

    private String status;
}
