package com.salahtech.BarberShop_Apis.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.salahtech.BarberShop_Apis.Dtos.UserProfileDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salahtech.BarberShop_Apis.Dtos.ProfileIncompleteError;
import com.salahtech.BarberShop_Apis.Services.UserProfileService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProfileCompletenessInterceptor implements HandlerInterceptor {

    @Autowired
    private UserProfileService userProfileService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {

        // Exclure certains endpoints
        String path = request.getRequestURI();
        if (path.contains("/auth/") || path.contains("/profile/complete") ||
                path.contains("/swagger") || path.contains("/actuator")) {
            return true;
        }

        // Vérifier si l'utilisateur est authentifié
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() &&
                !(auth.getPrincipal() instanceof String)) {

            String email = auth.getName();
            UserProfileDto profile = userProfileService.getCompleteProfile(email);

            if (!profile.isProfileComplete()) {
                response.setStatus(428);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"PROFILE_INCOMPLETE\",\"message\":\"Veuillez compléter votre profil\"}");
                return false;
            }
        }

        return true;
    }

    private boolean isExcludedPath(String path) {
        return path.contains("/auth/") ||
                path.contains("/profile/complete") ||
                path.contains("/profile/me") ||
                path.contains("/swagger") ||
                path.contains("/actuator") ||
                path.contains("/error");
    }

    private void sendProfileIncompleteResponse(HttpServletResponse response, String path)
            throws IOException {
        // Utiliser HttpStatus.PRECONDITION_FAILED (412) ou BAD_REQUEST (400)
        response.setStatus(HttpStatus.PRECONDITION_FAILED.value()); // 412
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ProfileIncompleteError error = ProfileIncompleteError.builder()
                .error("PROFILE_INCOMPLETE")
                .message("Veuillez compléter votre profil")
                .timestamp(LocalDateTime.now())
                .path(path)
                .redirectTo("/profile/complete")
                .build();

        response.getWriter().write(new ObjectMapper().writeValueAsString(error));
    }
}
