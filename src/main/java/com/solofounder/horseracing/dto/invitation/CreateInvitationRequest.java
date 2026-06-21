package com.solofounder.horseracing.dto.invitation;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvitationRequest {

    @NotNull(message = "Race registration id is required")
    private Long raceRegistrationId;

    @NotNull(message = "Jockey id is required")
    private Long jockeyId;

    private String message;
}
