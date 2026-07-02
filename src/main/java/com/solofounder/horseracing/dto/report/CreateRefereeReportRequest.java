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

    private Long entryId;

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
