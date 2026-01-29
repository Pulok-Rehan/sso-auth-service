package com.bracepl.reporting_service.application.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceResponse {
    private boolean hasError;
    private String message;
    private Object content;
    private String statusCode;

    public ServiceResponse(String message, String statusCode){
        this.hasError = true;
        this.message = message;
        this.content = null;
        this.statusCode = statusCode;
    }

    public ServiceResponse(String message, Object content, String statusCode){
        this.hasError = false;
        this.message = message;
        this.content = content;
        this.statusCode = statusCode;
    }
}
