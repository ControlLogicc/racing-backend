package com.solofounder.horseracing.model.converter;

import com.solofounder.horseracing.model.enums.RaceStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RaceStatusConverter implements AttributeConverter<RaceStatus, String> {

    @Override
    public String convertToDatabaseColumn(RaceStatus status) {
        if (status == null) {
            return null;
        }
        switch (status) {
            case OPEN_FOR_ENTRY:
                return "registration_open";
            case CLOSED_FOR_ENTRY:
                return "registration_closed";
            case RESULT_PENDING:
                return "provisional_result";
            case OFFICIAL:
                return "official_result";
            default:
                return status.name().toLowerCase();
        }
    }

    @Override
    public RaceStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String trimmed = dbData.trim().toLowerCase();
        switch (trimmed) {
            case "registration_open":
                return RaceStatus.OPEN_FOR_ENTRY;
            case "registration_closed":
                return RaceStatus.CLOSED_FOR_ENTRY;
            case "provisional_result":
                return RaceStatus.RESULT_PENDING;
            case "official_result":
                return RaceStatus.OFFICIAL;
            default:
                for (RaceStatus s : RaceStatus.values()) {
                    if (s.name().equalsIgnoreCase(trimmed)) {
                        return s;
                    }
                }
                throw new IllegalArgumentException("Unknown database value for RaceStatus: " + dbData);
        }
    }
}
