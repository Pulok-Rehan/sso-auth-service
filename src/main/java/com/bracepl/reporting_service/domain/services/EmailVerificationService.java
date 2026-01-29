package com.bracepl.reporting_service.domain.services;

import com.bracepl.reporting_service.adapter.in.entity.EmailVerification;
import com.bracepl.reporting_service.adapter.out.interfaces.EmailVerificationRepository;
import com.bracepl.reporting_service.adapter.out.interfaces.ModeratorRepository;
import com.bracepl.reporting_service.adapter.out.interfaces.ParticipantRepository;
import com.bracepl.reporting_service.adapter.out.service.OtpClient;
import com.bracepl.reporting_service.application.model.Moderator;
import com.bracepl.reporting_service.application.model.Participant;
import com.bracepl.reporting_service.application.model.ServiceResponse;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import com.bracepl.reporting_service.application.utils.exceptions.OtpNotFoundException;
import com.bracepl.reporting_service.application.utils.exceptions.OtpNotMatchException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class EmailVerificationService {
    private final EmailVerificationRepository emailVerificationRepository;
    private final ParticipantRepository participantRepository;
    private final ModeratorRepository moderatorRepository;
    private final OtpClient otpClient;

    private ServiceResponse verifyEmail(String email, String otp, String userType){
        if (otp == null || otp.isEmpty()){
            otpClient.sendOtp("", email);
            throw new OtpNotFoundException("Otp sent to email: " + email);
        }
        if (!otpClient.validateOtp("", email, otp)){
            throw new OtpNotMatchException("Otp did not match");
        }
        if (userType.equals("Participant")){
            Optional<Participant> participantOptional = participantRepository.findByEmail(email);
            if (participantOptional.isEmpty()){
                throw new NotFoundException("Could not find User with this email "+ email);
            }
            participantOptional.get().setIsVerified(true);
            participantRepository.save(participantOptional.get());
        }
        else {
            Optional<Moderator> moderatorOptional = moderatorRepository.findByEmail(email);
            if (moderatorOptional.isEmpty()) {
                throw new NotFoundException("Could not find User with this email " + email);
            }
            moderatorOptional.get().setIsVerified(true);
            moderatorRepository.save(moderatorOptional.get());
        }
        return new ServiceResponse("Email verification successful", email , "200");
    }
}
