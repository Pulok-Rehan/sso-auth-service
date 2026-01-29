package com.bracepl.reporting_service.application.model;

/**
 * Types of actions that trigger callbacks
 */
public enum CallbackAction {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete");
    
    private final String value;
    
    CallbackAction(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static CallbackAction fromValue(String value) {
        for (CallbackAction action : values()) {
            if (action.value.equals(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown CallbackAction: " + value);
    }
}