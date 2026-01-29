package com.bracepl.reporting_service.application.model;

import java.util.Arrays;

public enum UserType {
    ADMIN("Admin"),
    PARTICIPANT("Participant"),
    MODERATOR("Moderator"),
    SUPERADMIN("SuperAdmin");

    private final String value;

    UserType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserType fromValue(String value) {
        for (UserType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown UserType: " + value);
    }

    public static boolean isValidUserType(String userType) {
        if (userType == null) {
            return false;
        }

        return Arrays.stream(UserType.values())
                .anyMatch(e -> e.name().equalsIgnoreCase(userType));
    }
}