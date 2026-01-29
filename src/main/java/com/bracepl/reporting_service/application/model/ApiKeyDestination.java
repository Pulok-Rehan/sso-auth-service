package com.bracepl.reporting_service.application.model;

/**
 * Destination for API key in callback requests
 */
public enum ApiKeyDestination {
    HEADER("header"),
    BODY("body");
    
    private final String value;
    
    ApiKeyDestination(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static ApiKeyDestination fromValue(String value) {
        for (ApiKeyDestination destination : values()) {
            if (destination.value.equals(value)) {
                return destination;
            }
        }
        throw new IllegalArgumentException("Unknown ApiKeyDestination: " + value);
    }
}