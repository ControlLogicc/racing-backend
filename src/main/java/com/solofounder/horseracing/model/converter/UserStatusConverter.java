package com.solofounder.horseracing.model.converter;

import com.solofounder.horseracing.model.enums.UserStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class UserStatusConverter implements AttributeConverter<UserStatus, String> {

    @Override
    public String convertToDatabaseColumn(UserStatus status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case PENDING -> "pending";
            case ACTIVE -> "active";
            case REJECTED -> "rejected";
            case SUSPENDED -> "suspended";
            case INACTIVE -> "inactive";
            case BLOCKED -> "blocked";
        };
    }

    @Override
    public UserStatus convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }

        return switch (value.toLowerCase()) {
            case "pending" -> UserStatus.PENDING;
            case "active" -> UserStatus.ACTIVE;
            case "rejected" -> UserStatus.REJECTED;
            case "suspended" -> UserStatus.SUSPENDED;
            case "inactive" -> UserStatus.INACTIVE;
            case "blocked" -> UserStatus.BLOCKED;
            default -> throw new IllegalArgumentException("Unknown status value from database: " + value);
        };
    }
}
