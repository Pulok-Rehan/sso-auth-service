package com.bracepl.reporting_service.application.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Destination for API key in callback requests
 */
public enum ApiKeyDestination {
    HEADER("header"),
    BODY("body"),
    EMPTYSTRING("");
    
    private final String value;
    
    ApiKeyDestination(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static ApiKeyDestination fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ApiKeyDestination destination : values()) {
            if (destination.value.equalsIgnoreCase(value)) {
                return destination;
            }
        }
        // Optional: Return null for empty string if you prefer null over EMPTYSTRING enum
        if (value.isEmpty()) {
            return null;
        }
        throw new IllegalArgumentException("Unknown ApiKeyDestination: " + value);
    }
}