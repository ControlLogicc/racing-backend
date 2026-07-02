package com.solofounder.horseracing.dto.report;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefereeReportResponse {
    private Long reportId;
    private Long raceId;
    private String raceName;
    private Long entryId;
    private Long horseId;
    private String horseName;
    private Long jockeyId;
    private String jockeyName;
    private Long refereeId;
    private String refereeName;
    private String reportType;
    private String content;
    private String violations;
    private String decisions;
    private String description;
    private String decision;
    private String penalty;
    private String reportStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
