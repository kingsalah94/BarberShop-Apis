package com.salahtech.BarberShop_Apis.Config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.salahtech.BarberShop_Apis.Services.AuditLogService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

@Component
@RequiredArgsConstructor
public class LoginAuditListener implements ApplicationListener<AuthenticationSuccessEvent> {

    private final AuditLogService auditLogService;

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();

        if (principal instanceof ApplicationUser) {
            ApplicationUser user = (ApplicationUser) principal;

            // Récupérer IP pour le login aussi
            String ip = "unknown";
            try {
                ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attr != null)
                    ip = attr.getRequest().getRemoteAddr();
            } catch (Exception e) {
            }

            auditLogService.log(
                    user.getId(),
                    "LOGIN",
                    "SESSION",
                    user.getEmail(),
                    "User logged in successfully",
                    ip);
        }
    }
}