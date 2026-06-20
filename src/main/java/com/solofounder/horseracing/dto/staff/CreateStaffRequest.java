package com.solofounder.horseracing.dto.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStaffRequest {

    @NotNull(message = "User id is required")
    private Long userId;

    @NotBlank(message = "Staff code is required")
    private String staffCode;

    @NotBlank(message = "Department is required")
    private String department;

    private String status;
}
