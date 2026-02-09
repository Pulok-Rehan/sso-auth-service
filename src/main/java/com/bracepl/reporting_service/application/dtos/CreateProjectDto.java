package com.bracepl.reporting_service.application.dtos;

import com.bracepl.reporting_service.application.model.Project;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateProjectDto {
    private String credential;
    @Valid
    private Project project;

}
