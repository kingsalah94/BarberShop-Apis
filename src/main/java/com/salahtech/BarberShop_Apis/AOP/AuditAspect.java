package com.salahtech.BarberShop_Apis.AOP;

import com.salahtech.BarberShop_Apis.Services.AuditLogService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;

    // Cette méthode s'active APRÈS le succès d'une méthode annotée @LogActivity
    @AfterReturning(value = "@annotation(logActivity)", returning = "result")
    public void logAction(JoinPoint joinPoint, LogActivity logActivity, Object result) {
        try {
            // 1. Récupérer l'ID de l'utilisateur connecté
            Long userId = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            // On vérifie que le principal est bien ton ApplicationUser
            if (auth != null && auth.getPrincipal() instanceof ApplicationUser) {
                userId = ((ApplicationUser) auth.getPrincipal()).getId();
            }

            // 2. Récupérer l'adresse IP (Grâce à RequestContextHolder)
            String ip = "unknown";
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    ip = request.getRemoteAddr();
                    // Gestion proxy (optionnel, si tu es derrière Nginx)
                    String xForwardedFor = request.getHeader("X-Forwarded-For");
                    if (xForwardedFor != null) {
                        ip = xForwardedFor.split(",")[0];
                    }
                }
            } catch (Exception e) { /* Ignorer si hors contexte web */ }

            // 3. Récupérer l'ID de l'objet modifié (ex: ID du Booking créé)
            String subjectId = "UNKNOWN";
            if (result != null) {
                try {
                    // On utilise la réflexion pour trouver une méthode getId() sur l'objet retourné
                    var method = result.getClass().getMethod("getId");
                    Object id = method.invoke(result);
                    if (id != null) subjectId = String.valueOf(id);
                } catch (Exception e) { 
                    // Si l'objet retourné n'a pas d'ID (ex: void), on laisse UNKNOWN
                }
            }

            // 4. Appel du service d'écriture
            auditLogService.log(
                userId,
                logActivity.action(),
                logActivity.entityName(),
                subjectId,
                "Auto-generated via AOP", // Tu pourrais sérialiser les arguments ici si besoin
                ip
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
