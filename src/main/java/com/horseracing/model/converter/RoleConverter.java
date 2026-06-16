package com.horseracing.model.converter;

import com.horseracing.model.enums.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role role) {
        if (role == null) {
            return null;
        }
        return role.name().toLowerCase();
    }

    @Override
    public Role convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String trimmed = dbData.trim();
        for (Role r : Role.values()) {
            if (r.name().equalsIgnoreCase(trimmed)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown database value for Role: " + dbData);
    }
}
