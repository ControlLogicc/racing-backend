package com.solofounder.horseracing.dto.auth;

import com.solofounder.horseracing.model.enums.Role;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private Role role;
}
