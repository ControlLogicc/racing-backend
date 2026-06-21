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
        switch (status) {
            case SENT:
                return "sent";
            case PENDING:
                return "pending_response";
            case ACCEPTED:
                return "accepted";
            case DECLINED:
                return "declined";
            default:
                throw new IllegalArgumentException("Unknown RaceInvitationStatus: " + status);
        }
    }

    @Override
    public RaceInvitationStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String normalized = dbData.trim().toLowerCase();
        switch (normalized) {
            case "sent":
                return RaceInvitationStatus.SENT;
            case "pending":
            case "pending_response":
            case "draft":
                return RaceInvitationStatus.PENDING;
            case "accepted":
            case "used":
                return RaceInvitationStatus.ACCEPTED;
            case "declined":
            case "cancelled":
            case "expired":
                return RaceInvitationStatus.DECLINED;
            default:
                throw new IllegalArgumentException("Unknown database value for RaceInvitationStatus: " + dbData);
        }
    }
}
