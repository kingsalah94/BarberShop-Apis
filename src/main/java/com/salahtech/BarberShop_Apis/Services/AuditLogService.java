package com.salahtech.BarberShop_Apis.Services;

import com.salahtech.BarberShop_Apis.models.AuditEvent;
import com.salahtech.BarberShop_Apis.reppsitories.AuditEventRepository; // Note: j'utilise ton package 'reppsitories'
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditEventRepository repository;

    @Async // Exécute cette méthode dans un thread séparé
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Crée une nouvelle transaction indépendante
    public void log(Long userId, String action, String subjectType, String subjectId, String metadata, String ip) {
        try {
            AuditEvent event = AuditEvent.builder()
                    .userId(userId) // Adapté à ton entité (Long)
                    .action(action)
                    .subjectType(subjectType)
                    .subjectId(subjectId)
                    .metadataJson(metadata)
                    .ip(ip) // Adapté à ton entité (String)
                    .ts(OffsetDateTime.now()) // Adapté à ton entité (OffsetDateTime)
                    .build();

            repository.save(event);
        } catch (Exception e) {
            // En cas d'erreur d'audit, on l'affiche juste dans la console pour ne pas
            // bloquer l'appli
            System.err.println("❌ Erreur lors de l'enregistrement de l'audit : " + e.getMessage());
        }
    }
}
