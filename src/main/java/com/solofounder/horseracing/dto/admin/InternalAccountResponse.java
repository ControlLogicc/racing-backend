package com.solofounder.horseracing.dto.admin;

import com.solofounder.horseracing.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalAccountResponse {
    private Long userId;
    private Long profileId;
    private Role role;
    private String fullName;
    private String email;
    private String status;
}
