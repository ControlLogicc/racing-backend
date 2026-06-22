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
        switch (status) {
            case PENDING:
                return "pending";
            case APPROVED:
                return "approved";
            case REJECTED:
                return "rejected";
            default:
                throw new IllegalArgumentException("Unknown RaceRegistrationStatus: " + status);
        }
    }

    @Override
    public RaceRegistrationStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String trimmed = dbData.trim().toLowerCase();
        switch (trimmed) {
            case "pending":
            case "pending_review":
            case "submitted":
            case "draft":
                return RaceRegistrationStatus.PENDING;
            case "approved":
            case "converted_to_entry":
                return RaceRegistrationStatus.APPROVED;
            case "rejected":
            case "withdrawn":
                return RaceRegistrationStatus.REJECTED;
            default:
                throw new IllegalArgumentException("Unknown database value for RaceRegistrationStatus: " + dbData);
        }
    }
}
