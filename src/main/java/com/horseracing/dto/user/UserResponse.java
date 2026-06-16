package com.horseracing.dto.user;

import com.horseracing.model.enums.Role;
import com.horseracing.model.enums.UserStatus;
import lombok.*;

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
}
