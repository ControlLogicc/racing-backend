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
    private Long refereeId;
    private String refereeName;
    private String reportType;
    private String content;
    private String violations;
    private String decisions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
