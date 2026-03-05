package com.bracepl.reporting_service.adapter.in;

import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.application.model.ServiceResponse;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import com.bracepl.reporting_service.domain.services.LoginAttemptService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/participant")
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptAdminController {

    private final LoginAttemptService loginAttemptService;

    @PostMapping("/update-status")
    public ServiceResponse unblockUser(
            Authentication authentication,
            @RequestBody UnblockUserRequest username) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} unblocking user: {}", admin.getNanoId(), username.getUsername());

            loginAttemptService.unblock(username.getUsername(), username.getUsername().toLowerCase());
            return new ServiceResponse("Status updated successfully", null, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (Exception e) {
            log.error("Error unblocking user", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @Data
    public static class UnblockUserRequest {
        private String username;
    }
}