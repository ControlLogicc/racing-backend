package com.solofounder.horseracing.dto.race;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceResponse {
    private Long raceId;
    private Long meetingId;
    private Long conditionId;
    private String conditionName;
    private Long staffId;
    private String staffName;
    private Long refereeId;
    private String refereeName;
    private String raceName;
    private Short raceNo;
    private LocalDateTime scheduledTime;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
