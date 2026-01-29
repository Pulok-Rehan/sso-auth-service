package com.bracepl.reporting_service.adapter.in;

import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.application.model.ServiceResponse;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import com.bracepl.reporting_service.domain.services.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/login-attempts")
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptAdminController {

    private final LoginAttemptService loginAttemptService;

    @PostMapping("/unblock")
    public ServiceResponse unblockUser(
            Authentication authentication,
            @RequestParam String username) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} unblocking user: {}", admin.getId(), username);

            loginAttemptService.unblock(username, username);
            return new ServiceResponse("User unblocked successfully", null, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (Exception e) {
            log.error("Error unblocking user", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }
}