package com.bracepl.reporting_service.adapter.in;

import com.bracepl.reporting_service.adapter.out.model.ForgotPasswordRequest;
import com.bracepl.reporting_service.adapter.out.model.RegisterResponse;
import com.bracepl.reporting_service.adapter.out.model.ResetPasswordRequest;
import com.bracepl.reporting_service.adapter.out.model.ResetPasswordResponse;
import com.bracepl.reporting_service.application.dtos.LoginRequest;
import com.bracepl.reporting_service.application.dtos.ParticipantLoginResponse;
import com.bracepl.reporting_service.application.dtos.ParticipantRegistrationRequest;
import com.bracepl.reporting_service.application.model.AuthSession;
import com.bracepl.reporting_service.application.model.ServiceResponse;
import com.bracepl.reporting_service.application.model.UserType;
import com.bracepl.reporting_service.application.utils.exceptions.*;
import com.bracepl.reporting_service.domain.services.AuthSessionService;
import com.bracepl.reporting_service.domain.services.ParticipantAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AuthController {
    
    private final ParticipantAuthService participantAuthService;
    private final AuthSessionService authSessionService;

    @PostMapping("/register")
    public ServiceResponse register(
            @RequestHeader("project-id") String projectId,
            @RequestHeader("session-secret") String sessionSecret,
            @Valid @RequestBody ParticipantRegistrationRequest request) {
        try {
            log.info("Participant registration request for email: {}", request.getEmail());
            AuthSession authSession = authSessionService.verifyAuthSession(projectId, sessionSecret);

            // Verify user type matches
            if (!"Participant".equals(authSession.getUserType())) {
                return new ServiceResponse("Invalid user type", "500");
            }
            RegisterResponse registerResponse = participantAuthService.register(request, authSession);
            return new ServiceResponse("Registration Successful", registerResponse, "200");
        } catch (UnauthorizedException e) {
            return new ServiceResponse("User unauthorized", "401");
        }
        catch (ConflictException e) {
            return new ServiceResponse(e.getMessage(), "409");

        }
        catch (OtpNotFoundException e) {
            return new ServiceResponse("Otp sent to " +request.getEmail()+ " and " + request.getMobileNumber(), "427");
        }
        catch (OtpNotMatchException e) {
            return new ServiceResponse("OTP did not match", "428");
        }
        catch (Exception e) {
            return new ServiceResponse("Something went wrong", "500");
        }
    }
    @PostMapping("/login")
    public ServiceResponse login(
            @RequestHeader("project-id") String projectId,
            @RequestHeader("session-secret") String sessionSecret,
            @Valid @RequestBody LoginRequest request) {

        try {
            log.info("Participant login request for email: {}", request.getUsername());
            AuthSession authSession = authSessionService.verifyAuthSession(projectId, sessionSecret);

            if (UserType.isValidUserType(request.getUserType())) {
                ParticipantLoginResponse response = participantAuthService.login(request, authSession);
                if (response == null){
                    return new ServiceResponse("Something went wrong", "500");
                }
                return new ServiceResponse("Login Successful", response, "200");
            }
            else {
                return new ServiceResponse("Invalid user type", "500");
            }
        }
        catch (InvalidCredentialsException e) {
            return new ServiceResponse("Inavalid Credentials", "403");
        }
        catch (UnauthorizedException e) {
            return new ServiceResponse("User unauthorized", "401");
        }
        catch (FirstTimeLogin e) {
            return new ServiceResponse("You need to reset your password", "429");
        }
        catch (UserBlockedException e) {
            return new ServiceResponse("User is locked. Please contact our customer care", "430");
        }
        catch (NotFoundException e) {
            return new ServiceResponse("Could not find user", "501");
        }
        catch (Exception e) {
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @PostMapping("/reset-password")
    public ServiceResponse resetPassword(
            @RequestHeader("project-id") String projectId,
            @RequestHeader("session-secret") String sessionSecret,
            @Valid @RequestBody ResetPasswordRequest request) {

        try {
            log.info("Participant login request for email: {}", request.getUsername());
            AuthSession authSession = authSessionService.verifyAuthSession(projectId, sessionSecret);

            if ("Participant".equals(authSession.getUserType()) || "Moderator".equals(authSession.getUserType()) || "Admin".equals(authSession.getUserType())) {
                ResetPasswordResponse response = participantAuthService.resetPassword(request, authSession);
                if (response == null){
                    return new ServiceResponse("Something went wrong", "500");
                }
                return new ServiceResponse("Password reset successful", response, "200");
            }
            else {
                return new ServiceResponse("Invalid user type", "500");
            }
        }
        catch (InvalidCredentialsException e) {
            return new ServiceResponse("Old Password did not match", "403");
        }catch (UnauthorizedException e) {
            return new ServiceResponse("User unauthorized", "401");
        }
        catch (FirstTimeLogin e) {
            return new ServiceResponse("You need to reset your password", "429");
        }
        catch (UserBlockedException e) {
            return new ServiceResponse("User is blocked. Please contact our customer care", "430");
        }
        catch (NotFoundException e) {
            return new ServiceResponse("Could not find user", "501");
        }
        catch (Exception e) {
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @PostMapping("/forgot-password")
    public ServiceResponse forgotPassword(
            @RequestHeader("project-id") String projectId,
            @RequestHeader("session-secret") String sessionSecret,
            @Valid @RequestBody ForgotPasswordRequest request) {

        try {
            log.info("Participant login request for email: {}", request.getEmail());
            AuthSession authSession = authSessionService.verifyAuthSession(projectId, sessionSecret);

            if ("Participant".equals(authSession.getUserType()) || "Moderator".equals(authSession.getUserType())) {
                ResetPasswordResponse response = participantAuthService.forgotPasswordOtpBased(request, authSession);
                if (response == null){
                    return new ServiceResponse("Something went wrong", "500");
                }
                return new ServiceResponse("Temporary password sent to "+ response.getMobileNumber() +" and " + response.getEmail(), response, "200");
            }
            else {
                return new ServiceResponse("Invalid user type", "500");
            }
        } catch (UnauthorizedException e) {
            return new ServiceResponse("User unauthorized", "401");
        }
        catch (FirstTimeLogin e) {
            return new ServiceResponse("Temporary password sent to " + request.getEmail(), "429");
        }
        catch (OtpNotFoundException e) {
            return new ServiceResponse("Otp sent to "+request.getEmail() +" and mobile number " + request.getMobileNumber(), "427");
        }
        catch (OtpNotMatchException e) {
            return new ServiceResponse("OTP did not match", "428");
        }
        catch (UserBlockedException e) {
            return new ServiceResponse("User is blocked. Please contact our customer care", "430");
        }
        catch (NotFoundException e) {
            return new ServiceResponse("Could not find user", "501");
        }
        catch (Exception e) {
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @PostMapping("/logout")
    public ServiceResponse logout(
            @RequestAttribute("userId") String userId,
            @RequestAttribute("userType") UserType userType) {
        try {
            log.info("Logout request for user: {}", userId);
            participantAuthService.logout(userId, userType.getValue());
            return new ServiceResponse("Logout successful", null, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (Exception e) {
            log.error("Error during logout", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }
}