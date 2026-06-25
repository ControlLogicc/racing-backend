package com.solofounder.horseracing.model.converter;

import com.solofounder.horseracing.model.enums.JockeyRaceRegistrationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class JockeyRaceRegistrationStatusConverter
        implements AttributeConverter<JockeyRaceRegistrationStatus, String> {

    @Override
    public String convertToDatabaseColumn(JockeyRaceRegistrationStatus status) {
        return status == null ? null : status.name().toLowerCase();
    }

    @Override
    public JockeyRaceRegistrationStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        for (JockeyRaceRegistrationStatus status : JockeyRaceRegistrationStatus.values()) {
            if (status.name().equalsIgnoreCase(dbData.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown database value for JockeyRaceRegistrationStatus: " + dbData);
    }
}
