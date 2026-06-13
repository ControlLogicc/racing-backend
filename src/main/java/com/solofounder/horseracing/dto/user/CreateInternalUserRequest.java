package com.solofounder.horseracing.dto.user;

import com.solofounder.horseracing.model.enums.Role;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInternalUserRequest {
    private String fullName;
    private String email;
    private String password;
    private Role role;
}
