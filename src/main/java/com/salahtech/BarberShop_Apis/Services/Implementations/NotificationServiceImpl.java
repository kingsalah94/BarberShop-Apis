package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.google.firebase.messaging.FirebaseMessaging;
import com.salahtech.BarberShop_Apis.Dtos.NotificationDto;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.EmailService;
import com.salahtech.BarberShop_Apis.Services.SMSService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.NotificationService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.OutboxService;
import com.salahtech.BarberShop_Apis.models.AuditEvent;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.reppsitories.AuditEventRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final EmailService emailService;
    private final SMSService smsService;
    private final PushNotificationService pushNotificationService;
    private final AuditEventRepository auditEventRepository;
    private final OutboxService outboxService;
    private final BookingRepository bookingRepository;
    private final PushNotificationService pushService;

    // Formatter pour les dates dans les messages
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM à HH:mm");

    @Override
    @Async
    public CompletableFuture<Void> sendBookingConfirmation(Booking booking) {
        return CompletableFuture.runAsync(() -> {
            log.info("Envoi de confirmation de réservation pour la réservation {}", booking.getId());

            try {
                Map<String, Object> templateVariables = createBookingTemplateVariables(booking);
                boolean atLeastOneSent = false;

                // Email de confirmation
                if (booking.getClient() != null && booking.getClient().getEmail() != null) {
                    try {
                        emailService.sendTemplateEmail(
                                booking.getClient().getEmail(),
                                "Confirmation de votre réservation",
                                "booking-confirmation",
                                templateVariables);
                        atLeastOneSent = true;
                        log.debug("Email de confirmation envoyé à {}", booking.getClient().getEmail());
                    } catch (Exception e) {
                        log.error("Échec envoi email pour booking {}: {}", booking.getId(), e.getMessage());
                    }
                }

                // SMS de confirmation
                if (booking.getClient() != null && booking.getClient().getPhone() != null) {
                    try {
                        String smsMessage = String.format(
                                "Réservation confirmée chez %s le %s à %s. Réf: #%d",
                                booking.getBarber().getSalonName(),
                                booking.getStartAt().toLocalDate(),
                                booking.getStartAt().toLocalTime(),
                                booking.getId());
                        smsService.sendSMS(booking.getClient().getPhone(), smsMessage);
                        atLeastOneSent = true;
                        log.debug("SMS de confirmation envoyé à {}", booking.getClient().getPhone());
                    } catch (Exception e) {
                        log.error("Échec envoi SMS pour booking {}: {}", booking.getId(), e.getMessage());
                    }
                }

                // Push notification
                if (booking.getClient() != null && booking.getClient().getUser() != null) {
                    try {
                        pushNotificationService.sendToUser(
                                booking.getClient().getUser().getId(),
                                "Réservation confirmée",
                                "Votre réservation a été confirmée avec succès",
                                createNotificationData(booking));
                        atLeastOneSent = true;
                        log.debug("Push notification envoyée pour user {}", booking.getClient().getUser().getId());
                    } catch (Exception e) {
                        log.error("Échec envoi push pour booking {}: {}", booking.getId(), e.getMessage());
                    }
                }

                // Si au moins une notification a été envoyée avec succès
                if (atLeastOneSent) {
                    auditSuccessfulNotification(booking, "BOOKING_CONFIRMATION");
                } else {
                    throw new RuntimeException("Aucune notification n'a pu être envoyée");
                }

            } catch (Exception e) {
                handleNotificationFailure(booking, "BOOKING_CONFIRMATION", e);
            }
        });
    }

    @Override
    @Async
    public CompletableFuture<Void> sendBookingReminder(Booking booking) {
        return CompletableFuture.runAsync(() -> {
            log.info("Envoi de rappel de réservation pour la réservation {}", booking.getId());

            try {
                Map<String, Object> templateVariables = createBookingTemplateVariables(booking);
                boolean atLeastOneSent = false;

                // Email de rappel
                if (booking.getClient() != null && booking.getClient().getEmail() != null) {
                    try {
                        emailService.sendTemplateEmail(
                                booking.getClient().getEmail(),
                                "Rappel - Votre rendez-vous approche",
                                "booking-reminder",
                                templateVariables);
                        atLeastOneSent = true;
                    } catch (Exception e) {
                        log.error("Échec envoi email rappel: {}", e.getMessage());
                    }
                }

                // SMS de rappel
                if (booking.getClient() != null && booking.getClient().getPhone() != null) {
                    try {
                        String smsMessage = String.format(
                                "Rappel: RDV demain chez %s à %s. Réf: #%d",
                                booking.getBarber().getSalonName(),
                                booking.getStartAt().toLocalTime(),
                                booking.getId());
                        smsService.sendSMS(booking.getClient().getPhone(), smsMessage);
                        atLeastOneSent = true;
                    } catch (Exception e) {
                        log.error("Échec envoi SMS rappel: {}", e.getMessage());
                    }
                }

                // Push notification
                if (booking.getClient() != null && booking.getClient().getUser() != null) {
                    try {
                        pushNotificationService.sendToUser(
                                booking.getClient().getUser().getId(),
                                "Rappel de rendez-vous",
                                "N'oubliez pas votre rendez-vous demain",
                                createNotificationData(booking));
                        atLeastOneSent = true;
                    } catch (Exception e) {
                        log.error("Échec envoi push rappel: {}", e.getMessage());
                    }
                }

                if (atLeastOneSent) {
                    auditSuccessfulNotification(booking, "BOOKING_REMINDER");
                } else {
                    throw new RuntimeException("Aucun rappel n'a pu être envoyé");
                }

            } catch (Exception e) {
                handleNotificationFailure(booking, "BOOKING_REMINDER", e);
            }
        });
    }

    @Override
    @Async
    public CompletableFuture<Void> sendBookingCancellation(Booking booking) {
        return CompletableFuture.runAsync(() -> {
            log.info("Envoi de notification d'annulation pour la réservation {}", booking.getId());

            try {
                Map<String, Object> templateVariables = createBookingTemplateVariables(booking);
                boolean atLeastOneSent = false;

                // Notifier le client
                if (booking.getClient() != null && booking.getClient().getEmail() != null) {
                    try {
                        emailService.sendTemplateEmail(
                                booking.getClient().getEmail(),
                                "Annulation de votre réservation",
                                "booking-cancellation",
                                templateVariables);
                        atLeastOneSent = true;
                    } catch (Exception e) {
                        log.error("Échec envoi email annulation client: {}", e.getMessage());
                    }
                }

                // Notifier le barbier
                if (booking.getBarber() != null && booking.getBarber().getUser() != null
                        && booking.getBarber().getUser().getEmail() != null) {
                    try {
                        emailService.sendTemplateEmail(
                                booking.getBarber().getUser().getEmail(),
                                "Réservation annulée",
                                "booking-cancellation-barber",
                                templateVariables);
                        atLeastOneSent = true;
                    } catch (Exception e) {
                        log.error("Échec envoi email annulation barbier: {}", e.getMessage());
                    }
                }

                if (atLeastOneSent) {
                    auditSuccessfulNotification(booking, "BOOKING_CANCELLATION");
                } else {
                    throw new RuntimeException("Aucune notification d'annulation n'a pu être envoyée");
                }

            } catch (Exception e) {
                handleNotificationFailure(booking, "BOOKING_CANCELLATION", e);
            }
        });
    }

    @Override
    @Async
    public CompletableFuture<Void> sendCustomNotification(NotificationDto notification) {
        return CompletableFuture.runAsync(() -> {
            log.info("Envoi de notification personnalisée de type {}", notification.getType());

            try {
                switch (notification.getType()) {
                    case EMAIL -> emailService.sendSimpleEmail(
                            notification.getRecipient(),
                            notification.getSubject(),
                            notification.getMessage());
                    case SMS -> smsService.sendSMS(
                            notification.getRecipient(),
                            notification.getMessage());
                    case PUSH -> pushNotificationService.sendToUser(
                            Long.valueOf(notification.getRecipient()),
                            notification.getSubject(),
                            notification.getMessage(),
                            notification.getData());
                    default -> throw new IllegalArgumentException(
                            "Type de notification non supporté: " + notification.getType());
                }

                // Audit succès
                auditEventRepository.save(AuditEvent.builder()
                        .ts(OffsetDateTime.now())
                        .action("CUSTOM_NOTIFICATION_SENT")
                        .subjectType(notification.getType().toString())
                        .subjectId(notification.getRecipient())
                        .metadataJson(String.format("{\"type\":\"%s\",\"status\":\"SUCCESS\"}", notification.getType()))
                        .build());

            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de la notification personnalisée: {}", e.getMessage(), e);

                // Audit échec
                auditEventRepository.save(AuditEvent.builder()
                        .ts(OffsetDateTime.now())
                        .action("CUSTOM_NOTIFICATION_FAILED")
                        .subjectType(notification.getType().toString())
                        .subjectId(notification.getRecipient())
                        .metadataJson(String.format("{\"error\":\"%s\"}", e.getMessage().replace("\"", "\\\"")))
                        .build());
            }
        });
    }

    // Méthodes privées helper

    private void auditSuccessfulNotification(Booking booking, String notificationType) {
        auditEventRepository.save(AuditEvent.builder()
                .ts(OffsetDateTime.now())
                .userId(booking.getClient() != null && booking.getClient().getUser() != null
                        ? booking.getClient().getUser().getId()
                        : null)
                .action("NOTIFICATION_SENT")
                .subjectType("BOOKING")
                .subjectId(booking.getId().toString())
                .metadataJson(String.format("{\"type\":\"%s\",\"status\":\"SUCCESS\"}", notificationType))
                .build());
    }

    private void handleNotificationFailure(Booking booking, String notificationType, Exception e) {
        log.error("Erreur lors de l'envoi de {} pour booking {}: {}",
                notificationType, booking.getId(), e.getMessage(), e);

        // Audit de l'échec
        auditEventRepository.save(AuditEvent.builder()
                .ts(OffsetDateTime.now())
                .userId(booking.getClient() != null && booking.getClient().getUser() != null
                        ? booking.getClient().getUser().getId()
                        : null)
                .action("NOTIFICATION_FAILED")
                .subjectType("BOOKING")
                .subjectId(booking.getId().toString())
                .metadataJson(String.format(
                        "{\"type\":\"%s\",\"error\":\"%s\"}",
                        notificationType,
                        e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "Unknown error"))
                .build());

        // Enqueue pour retry via Outbox
        enqueueRetryNotification(booking, notificationType);
    }

    private void enqueueRetryNotification(Booking booking, String notificationType) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("bookingId", booking.getId());
            payload.put("notificationType", notificationType);
            payload.put("clientId", booking.getClient() != null ? booking.getClient().getId() : null);
            payload.put("clientEmail", booking.getClient() != null ? booking.getClient().getEmail() : null);
            payload.put("clientPhone", booking.getClient() != null ? booking.getClient().getPhone() : null);
            payload.put("barberId", booking.getBarber() != null ? booking.getBarber().getId() : null);

            outboxService.add(
                    "NOTIFICATION_RETRY",
                    booking.getId().toString(),
                    notificationType,
                    payload,
                    Map.of("retryCount", 0, "maxRetries", 3, "nextRetryAt",
                            OffsetDateTime.now().plusMinutes(5).toString()));

            log.info("Notification mise en file d'attente pour retry: booking={}, type={}",
                    booking.getId(), notificationType);

        } catch (Exception ex) {
            log.error("Impossible d'enregistrer la notification pour retry: {}", ex.getMessage(), ex);
        }
    }

    private Map<String, Object> createBookingTemplateVariables(Booking booking) {
        Map<String, Object> variables = new HashMap<>();

        if (booking.getClient() != null) {
            variables.put("customerName", booking.getClient().getFullName());
            variables.put("customerEmail", booking.getClient().getEmail());
        }

        if (booking.getBarber() != null) {
            variables.put("barberName", booking.getBarber().getName());
            variables.put("salonName", booking.getBarber().getSalonName());
            variables.put("salonAddress", booking.getBarber().getLocation());
        }

        if (booking.getBarberService() != null) {
            variables.put("serviceName", booking.getBarberService().getName());
            variables.put("duration", booking.getBarberService().getDuration());
            variables.put("price", booking.getBarberService().getPrice());
        }

        variables.put("appointmentDate", booking.getStartAt() != null ? booking.getStartAt().toLocalDate() : null);
        variables.put("appointmentTime", booking.getStartAt() != null ? booking.getStartAt().toLocalTime() : null);
        variables.put("bookingId", booking.getId());
        variables.put("bookingStatus", booking.getStatus() != null ? booking.getStatus().toString() : "PENDING");

        return variables;
    }

    private Map<String, String> createNotificationData(Booking booking) {
        Map<String, String> data = new HashMap<>();
        data.put("bookingId", booking.getId().toString());
        data.put("type", "booking");
        data.put("barberId", booking.getBarber() != null ? booking.getBarber().getId().toString() : "");
        data.put("status", booking.getStatus() != null ? booking.getStatus().toString() : "");
        data.put("startAt", booking.getStartAt() != null ? booking.getStartAt().toString() : "");
        return data;
    }

    @Override
    public void sendNotification(String targetToken, String title, String body, Map<String, String> data) {
        if (targetToken == null || targetToken.isEmpty()) {
            log.warn("Tentative d'envoi de notif sans token FCM.");
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(targetToken)
                    .setNotification(notification)
                    .putAllData(data != null ? data : new HashMap<>()) // Données invisibles (ex: id du rdv pour cliquer
                                                                       // dessus)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Notification envoyée avec succès: " + response);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification FCM", e);
            // On ne lance pas d'exception pour ne pas bloquer le processus de réservation
        }
    }

    @Override
    public void sendBookingNotification(String targetToken, String title, String body, Long bookingId) {
        Map<String, String> data = new HashMap<>();
        data.put("bookingId", String.valueOf(bookingId));
        data.put("type", "BOOKING_UPDATE");

        sendNotification(targetToken, title, body, data);
    }

    // ==========================================
    // ✅ MÉTHODES "RESEND" (Trigger par ID)
    // ==========================================

    @Override
    public void resendBookingConfirmation(Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        sendBookingConfirmation(booking);
    }

    @Override
    public void resendBookingReminder(Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        sendBookingReminder(booking);
    }

    @Override
    public void resendBookingCancellation(Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        sendBookingCancellation(booking);
    }

    // --- Helper ---
    private Booking getBookingOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking introuvable ID: " + id));
    }
}