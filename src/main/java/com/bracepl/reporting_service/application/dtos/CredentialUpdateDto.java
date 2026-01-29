package com.bracepl.reporting_service.application.dtos;

import lombok.Data;

@Data
public class CredentialUpdateDto {
    private String projectId;
    private String credentialId;
    private String newSecret;
}
