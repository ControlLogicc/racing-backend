package com.solofounder.horseracing.dto.entry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "(?i)^(declared|passed|failed|withdrawn|disqualified|dnf|scratched|finished|running|ready|checked_in)$", message = "Invalid entry status")
    private String status;
}
