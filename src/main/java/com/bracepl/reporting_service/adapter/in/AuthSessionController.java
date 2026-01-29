package com.bracepl.reporting_service.adapter.in;

import com.bracepl.reporting_service.application.dtos.CreateAuthSessionRequest;
import com.bracepl.reporting_service.application.dtos.CreateAuthSessionResponse;
import com.bracepl.reporting_service.application.model.ServiceResponse;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import com.bracepl.reporting_service.application.utils.exceptions.UnauthorizedException;
import com.bracepl.reporting_service.domain.services.AuthSessionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/session")
@RequiredArgsConstructor
@Slf4j
public class AuthSessionController {
    
    private final AuthSessionService authSessionService;
    private final ObjectMapper objectMapper;

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ServiceResponse createAuthSession(
            @Valid @RequestBody CreateAuthSessionRequest request) {
        try {
            log.info("Received request to create auth session for project: {}", request.getProjectId());

            CreateAuthSessionResponse response = authSessionService.createAuthSession(request);

            return new ServiceResponse("Auth session created successfully", response, "200");

        }
        catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "501");
        }
        catch (UnauthorizedException e) {
            return new ServiceResponse("Invalid Credentials or User Type", "401");
        }
        catch (Exception e) {
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @GetMapping
    public String test(){
        return "API CALLED...";
    }
}