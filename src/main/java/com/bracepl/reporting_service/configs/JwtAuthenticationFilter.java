package com.bracepl.reporting_service.configs;

import com.bracepl.reporting_service.adapter.out.interfaces.*;
import com.bracepl.reporting_service.application.model.UserType;
import com.bracepl.reporting_service.application.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AdminRepository adminRepository;
    private final ParticipantRepository participantRepository;
    private final ModeratorRepository moderatorRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Claims claims = jwtUtil.verifyToken(token);

                String userId = claims.get("id", String.class);
                String publicKey = claims.get("public_key", String.class);
                String userTypeValue = claims.get("userType", String.class);
                UserType userType = UserType.fromValue(userTypeValue);

                Object user = loadUser(publicKey, userType);

                // Store in request attributes
                request.setAttribute("user", user);
                request.setAttribute("userType", userType);
                request.setAttribute("userId", userId);
                request.setAttribute("publicKey", publicKey);

                // Create Spring Security authentication
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                log.error("JWT validation failed", e);
                // Don't set authentication - let Spring Security deny access
            }
        }

        filterChain.doFilter(request, response);
    }

    private Object loadUser(String publicKey, UserType userType) {
        return switch (userType) {
            case ADMIN -> adminRepository.findByPublicKeyAndStatusTrue(publicKey)
                    .orElseThrow(() -> new RuntimeException("Admin not found or inactive"));
            case SUPERADMIN -> adminRepository.findByPublicKeyAndStatusTrue(publicKey)
                    .orElseThrow(() -> new RuntimeException("Admin not found or inactive"));
            case PARTICIPANT -> participantRepository.findByPublicKeyAndStatusTrue(publicKey)
                    .orElseThrow(() -> new RuntimeException("Participant not found or inactive"));
            case MODERATOR -> moderatorRepository.findByPublicKeyAndStatusTrue(publicKey)
                    .orElseThrow(() -> new RuntimeException("Moderator not found or inactive"));
        };
    }
}