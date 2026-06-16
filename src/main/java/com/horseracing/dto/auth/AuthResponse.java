package com.horseracing.dto.auth;

import com.horseracing.model.enums.Role;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
}
