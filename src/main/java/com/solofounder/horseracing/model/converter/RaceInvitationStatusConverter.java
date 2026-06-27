package com.solofounder.horseracing.model.converter;

import com.solofounder.horseracing.model.enums.RaceInvitationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RaceInvitationStatusConverter implements AttributeConverter<RaceInvitationStatus, String> {

    @Override
    public String convertToDatabaseColumn(RaceInvitationStatus status) {
        if (status == null) {
            return null;
        }
        return status.name().toLowerCase();
    }

    @Override
    public RaceInvitationStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String trimmed = dbData.trim();
        for (RaceInvitationStatus s : RaceInvitationStatus.values()) {
            if (s.name().equalsIgnoreCase(trimmed)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown database value for RaceInvitationStatus: " + dbData);
    }
}
