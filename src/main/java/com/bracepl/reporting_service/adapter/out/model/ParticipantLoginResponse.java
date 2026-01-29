package com.bracepl.reporting_service.adapter.out.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class ParticipantLoginResponse {
    
    private String token;
    private com.bracepl.reporting_service.application.dtos.ParticipantLoginResponse.ParticipantProfile profile;
    private String userType;
    private String uiCallback;
}