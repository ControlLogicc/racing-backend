package com.solofounder.horseracing.model.converter;

import com.solofounder.horseracing.model.enums.UserStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserStatusConverter implements AttributeConverter<UserStatus, String> {

    @Override
    public String convertToDatabaseColumn(UserStatus status) {
        if (status == null) {
            return null;
        }
        return status.name().toLowerCase();
    }

    @Override
    public UserStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String trimmed = dbData.trim();
        for (UserStatus s : UserStatus.values()) {
            if (s.name().equalsIgnoreCase(trimmed)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown database value for UserStatus: " + dbData);
    }
}
