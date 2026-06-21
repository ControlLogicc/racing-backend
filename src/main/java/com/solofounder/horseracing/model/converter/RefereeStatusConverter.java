package com.solofounder.horseracing.model.converter;

import com.solofounder.horseracing.model.enums.RefereeStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RefereeStatusConverter implements AttributeConverter<RefereeStatus, String> {

    @Override
    public String convertToDatabaseColumn(RefereeStatus status) {
        if (status == null) {
            return null;
        }
        return status.name().toLowerCase();
    }

    @Override
    public RefereeStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String trimmed = dbData.trim();
        for (RefereeStatus s : RefereeStatus.values()) {
            if (s.name().equalsIgnoreCase(trimmed)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown database value for RefereeStatus: " + dbData);
    }
}
