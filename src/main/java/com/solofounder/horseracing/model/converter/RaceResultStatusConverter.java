package com.solofounder.horseracing.model.converter;

import com.solofounder.horseracing.model.enums.RaceResultStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RaceResultStatusConverter implements AttributeConverter<RaceResultStatus, String> {

    @Override
    public String convertToDatabaseColumn(RaceResultStatus status) {
        if (status == null) {
            return null;
        }
        return status.name().toLowerCase();
    }

    @Override
    public RaceResultStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return RaceResultStatus.valueOf(dbData.trim().toUpperCase());
    }
}
