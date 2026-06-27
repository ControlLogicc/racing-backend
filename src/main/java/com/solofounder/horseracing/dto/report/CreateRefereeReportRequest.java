package com.solofounder.horseracing.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRefereeReportRequest {

    @NotNull(message = "Race id is required")
    private Long raceId;

    @NotBlank(message = "Report type is required")
    private String reportType;

    @NotBlank(message = "Content is required")
    private String content;

    private String violations;

    private String decisions;
}
