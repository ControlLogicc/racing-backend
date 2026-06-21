package com.solofounder.horseracing.model.converter;

import com.solofounder.horseracing.model.enums.RaceRegistrationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RaceRegistrationStatusConverter implements AttributeConverter<RaceRegistrationStatus, String> {

    @Override
    public String convertToDatabaseColumn(RaceRegistrationStatus status) {
        if (status == null) {
            return null;
        }
        return status.name().toLowerCase();
    }

    @Override
    public RaceRegistrationStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String trimmed = dbData.trim();
        for (RaceRegistrationStatus s : RaceRegistrationStatus.values()) {
            if (s.name().equalsIgnoreCase(trimmed)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown database value for RaceRegistrationStatus: " + dbData);
    }
}
