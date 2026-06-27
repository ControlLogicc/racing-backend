package com.solofounder.horseracing.dto.staff;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffResponse {
    private Long staffId;
    private Long userId;
    private String fullName;
    private String email;
    private String staffCode;
    private String department;
    private String status;
    private LocalDateTime createdAt;
}
