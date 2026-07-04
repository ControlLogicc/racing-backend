package com.solofounder.horseracing.dto.user;

import com.solofounder.horseracing.model.enums.Role;
import com.solofounder.horseracing.model.enums.UserStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private UserStatus status;
    private String accountStatus;
    private String bannedReason;
    private LocalDateTime bannedAt;
    private Long bannedBy;
}
