package com.solofounder.horseracing.model.converter;

import com.solofounder.horseracing.model.enums.RefereeReportType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RefereeReportTypeConverter implements AttributeConverter<RefereeReportType, String> {

    @Override
    public String convertToDatabaseColumn(RefereeReportType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case PRE_RACE:
                return "pre_race_check";
            case VIOLATION:
                return "violation";
            case DECISION:
                return "result_confirmation";
            default:
                throw new IllegalArgumentException("Unknown RefereeReportType: " + type);
        }
    }

    @Override
    public RefereeReportType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String trimmed = dbData.trim().toLowerCase();
        switch (trimmed) {
            case "pre_race":
            case "pre_race_check":
                return RefereeReportType.PRE_RACE;
            case "violation":
                return RefereeReportType.VIOLATION;
            case "decision":
            case "race_review":
            case "result_confirmation":
                return RefereeReportType.DECISION;
            default:
                throw new IllegalArgumentException("Unknown database value for RefereeReportType: " + dbData);
        }
    }
}
