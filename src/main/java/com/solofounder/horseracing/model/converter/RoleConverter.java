package com.solofounder.horseracing.model.converter;

import com.solofounder.horseracing.model.enums.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role role) {
        if (role == null) {
            return null;
        }
        return role.name();
    }

    @Override
    public Role convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String normalized = dbData.toUpperCase().trim();
        if (normalized.equals("OWNER")) {
            return Role.HORSE_OWNER;
        }
        if (normalized.equals("REFEREE")) {
            return Role.RACE_REFEREE;
        }
        try {
            return Role.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown role value in database: " + dbData);
        }
    }
}
