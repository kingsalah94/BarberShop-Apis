package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.salahtech.BarberShop_Apis.Dtos.NotificationDto;
import com.salahtech.BarberShop_Apis.Dtos.NotificationMessageDto; // ✅ Ajouté
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.EmailService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.NotificationService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.OutboxService;
import com.salahtech.BarberShop_Apis.Services.SMSService;
import com.salahtech.BarberShop_Apis.models.AuditEvent;
import com.salahtech.BarberShop_Apis.repositories.AuditEventRepository;
import com.salahtech.BarberShop_Apis.repositories.NotificationRepository; // ✅ Ajouté

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final EmailService emailService;
    private final SMSService smsService;
    private final PushNotificationService pushNotificationService;
    private final AuditEventRepository auditEventRepository;
    private final OutboxService outboxService;
    private final NotificationRepository notificationRepository; // ✅ Injecté pour la base de données

    // Formatter pour les dates dans les messages
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM à HH:mm");

    // ==============================================================
    // ✅ 1. MÉTHODE POUR LE DASHBOARD (GET)
    // ==============================================================
    @Override
    public List<NotificationMessageDto> getUserNotifications(Long userId) {
        // Récupère les notifs depuis la DB, mappe vers le DTO et renvoie la liste
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(50) // Les 50 dernières
                .map(n -> NotificationMessageDto.builder()
                        .id(n.getId())
                        .userId(n.getUserId())
                        .title(n.getTitle())
                        .body(n.getBody())
                        .type(n.getType())
                        .referenceId(n.getReferenceId())
                        .read(n.isRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ==============================================================
    // 5. NOTIFICATION PERSONNALISÉE (ADMIN)
    // ==============================================================
    @Override
    @Async
    public CompletableFuture<Void> sendCustomNotification(NotificationDto notification) {
        return CompletableFuture.runAsync(() -> {
            try {
                // ... (Votre switch case existant pour l'envoi technique) ...
                switch (notification.getType()) {
                    case EMAIL -> emailService.sendSimpleEmail(notification.getRecipient(), notification.getSubject(),
                            notification.getMessage());
                    // ... SMS, PUSH ...
                    default -> {
                    } // Géré dans votre code original
                }

                // ✅ SAUVEGARDE DB : Si c'est un User ID qu'on cible
                try {
                    Long recipientId = Long.valueOf(notification.getRecipient());
                    saveNotificationLog(
                            recipientId,
                            notification.getSubject(),
                            notification.getMessage(),
                            "INFO", // Type générique
                            null);
                } catch (NumberFormatException e) {
                    // Si recipient est un email, on ne peut pas sauver en DB liée à un User ID
                    // facilement ici sans lookup
                    log.warn("Notification custom envoyée à un email/tel, pas de log DB user: {}",
                            notification.getRecipient());
                }

                // Audit ...

            } catch (Exception e) {
                // Logs erreur...
            }
        });
    }

    // ==============================================================
    // ✅ 6. HELPER POUR SAUVEGARDER EN DB
    // ==============================================================
    private void saveNotificationLog(Long userId, String title, String body, String type, Long refId) {
        try {
            // On utilise le chemin complet pour éviter le conflit avec Firebase
            // Notification
            com.salahtech.BarberShop_Apis.models.Notification notif = com.salahtech.BarberShop_Apis.models.Notification
                    .builder()
                    .userId(userId)
                    .title(title)
                    .body(body)
                    .type(type)
                    .referenceId(refId)
                    .read(false)
                    .build();
            notificationRepository.save(notif);
        } catch (Exception e) {
            log.error("Erreur critique lors de la sauvegarde de la notification en base: {}", e.getMessage());
        }
    }

    // ==============================================================
    // MÉTHODES DE BASE & HELPERS EXISTANTS
    // ==============================================================

    @Override
    public void sendNotification(String targetToken, String title, String body, Map<String, String> data) {
        if (targetToken == null || targetToken.isEmpty())
            return;
        try {
            // Notification Firebase (Classe de Google)
            com.google.firebase.messaging.Notification notification = com.google.firebase.messaging.Notification
                    .builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(targetToken)
                    .setNotification(notification)
                    .putAllData(data != null ? data : new HashMap<>())
                    .build();

            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.error("Erreur FCM", e);
        }
    }

    @Override
    public void sendBookingNotification(String targetToken, String title, String body, Long bookingId) {
        Map<String, String> data = new HashMap<>();
        data.put("bookingId", String.valueOf(bookingId));
        data.put("type", "BOOKING_UPDATE");
        sendNotification(targetToken, title, body, data);
    }

    // ... Vos méthodes privées auditSuccessfulNotification,
    // handleNotificationFailure, etc. restent inchangées ...
    // Je ne les ai pas toutes recopiées ici pour gagner de la place, mais
    // gardez-les !

}