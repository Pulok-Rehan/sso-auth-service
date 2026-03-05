package com.bracepl.reporting_service.adapter.in;

import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.application.model.ServiceResponse;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import com.bracepl.reporting_service.domain.services.UserLookupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequestMapping("/api")
@RestController
@Slf4j
public class UserLookupController {
    private final UserLookupService userLookupService;

    public UserLookupController(UserLookupService userLookupService) {
        this.userLookupService = userLookupService;
    }


    @GetMapping("/user-lookup")
    public ServiceResponse lookupUser(
            Authentication authentication,
            @RequestParam String username,
            @RequestParam String userType) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} looking up user: {} type: {}", admin.getNanoId(), username, userType);

            Map<String, String> userInfo = userLookupService.lookupUser(username, userType);
            return new ServiceResponse("User found", userInfo, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (IllegalArgumentException e) {
            return new ServiceResponse(e.getMessage(), "400");
        } catch (Exception e) {
            log.error("Error looking up user", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }
}
