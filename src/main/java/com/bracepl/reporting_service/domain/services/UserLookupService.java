package com.bracepl.reporting_service.domain.services;

import com.bracepl.reporting_service.adapter.out.interfaces.ModeratorRepository;
import com.bracepl.reporting_service.adapter.out.interfaces.ParticipantRepository;
import com.bracepl.reporting_service.application.model.Moderator;
import com.bracepl.reporting_service.application.model.Participant;
import com.bracepl.reporting_service.application.model.UserType;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserLookupService {
    private final ParticipantRepository participantRepository;
    private final ModeratorRepository moderatorRepository;

    public UserLookupService(ParticipantRepository participantRepository, ModeratorRepository moderatorRepository) {
        this.participantRepository = participantRepository;
        this.moderatorRepository = moderatorRepository;
    }

    public Map<String, String> lookupUser(String username, String userTypeStr) {
        log.info("Looking up user: {} type: {}", username, userTypeStr);

        Map<String, String> result = new HashMap<>();

        if (UserType.PARTICIPANT.getValue().equalsIgnoreCase(userTypeStr)) {
            Participant participant = participantRepository.findActiveByUsername(username)
                    .orElseThrow(() -> new NotFoundException("Participant not found"));
            result.put("email", participant.getEmail());
            result.put("mobileNumber", participant.getMobileNumber());
            result.put("id", participant.getNanoId());
        } else if (UserType.MODERATOR.getValue().equalsIgnoreCase(userTypeStr)) {
            Moderator moderator = moderatorRepository.findActiveByUsername(username)
                    .orElseThrow(() -> new NotFoundException("Moderator not found"));
            result.put("email", moderator.getEmail());
            result.put("mobileNumber", moderator.getMobileNumber());
            result.put("id", moderator.getNanoId());
        } else {
            throw new IllegalArgumentException("Invalid user type for lookup. ");
        }

        return result;
    }
}
