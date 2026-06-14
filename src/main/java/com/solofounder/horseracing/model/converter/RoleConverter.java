package com.solofounder.horseracing.model.converter;

import com.solofounder.horseracing.model.enums.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role role) {
        if (role == null) {
            return null;
        }

        return switch (role) {
            case ADMIN -> "admin";
            case HORSE_OWNER -> "owner";
            case STAFF -> "staff";
            case RACE_REFEREE -> "referee";
            case JOCKEY -> "jockey";
            case SPECTATOR -> "spectator";
        };
    }

    @Override
    public Role convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }

        return switch (value.toLowerCase()) {
            case "admin" -> Role.ADMIN;
            case "owner" -> Role.HORSE_OWNER;
            case "staff" -> Role.STAFF;
            case "referee" -> Role.RACE_REFEREE;
            case "jockey" -> Role.JOCKEY;
            case "spectator" -> Role.SPECTATOR;
            default -> throw new IllegalArgumentException("Unknown role value from database: " + value);
        };
    }
}
