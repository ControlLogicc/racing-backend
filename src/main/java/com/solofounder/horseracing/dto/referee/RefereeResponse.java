package com.solofounder.horseracing.dto.referee;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefereeResponse {
    private Long refereeId;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String licenseNo;
    private String status;
    private LocalDateTime createdAt;
}
