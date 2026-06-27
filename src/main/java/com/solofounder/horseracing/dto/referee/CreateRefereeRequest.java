package com.solofounder.horseracing.dto.referee;
 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRefereeRequest {

    @NotNull(message = "User id is required")
    private Long userId;

    @NotBlank(message = "License number is required")
    private String licenseNo;
}
