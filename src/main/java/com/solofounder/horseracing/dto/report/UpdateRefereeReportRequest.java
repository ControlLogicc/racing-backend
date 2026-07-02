package com.solofounder.horseracing.dto.report;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRefereeReportRequest {

    @NotBlank(message = "Report type is required")
    private String reportType;

    private String content;

    private String violations;

    private String decisions;

    private String description;
    private String decision;
    private String penalty;
    private String reportStatus;
}
