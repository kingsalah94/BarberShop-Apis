package com.salahtech.BarberShop_Apis.notifications;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salahtech.BarberShop_Apis.Dtos.NotificationMessageDto;
import com.salahtech.BarberShop_Apis.Services.EmailService;
import com.salahtech.BarberShop_Apis.Services.Implementations.PushNotificationService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;
import com.salahtech.BarberShop_Apis.reppsitories.ProcessedMessageRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaConsumer {

  private final ObjectMapper objectMapper;
  private final BookingRepository bookingRepository;
  private final ProcessedMessageRepository processedRepo;
  private final EmailService emailService;
  private final PushNotificationService pushService;
  private final ApplicationUserRepository userRepository;

  // Formatteur pour afficher les dates proprement dans les notifs
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM 'à' HH:mm");

  /**
   * LISTENER 1 : Traite les événements métier (Outbox Pattern)
   * Déclenché quand une réservation change d'état en base de données.
   */
  @KafkaListener(topics = "${app.outbox.topic}", groupId = "notifications-service")
  @Transactional
  public void onMessage(OutboxMessage msg) throws Exception {
    UUID messageId = UUID.fromString(msg.id());

    // 1. Idempotence : On vérifie si le message a déjà été traité
    try {
      processedRepo.markProcessed(messageId);
    } catch (DataIntegrityViolationException dup) {
      log.info("Message dupliqué ignoré : {}", messageId);
      return;
    }

    // 2. Extraction des données
    JsonNode payload = objectMapper.readTree(msg.payload());
    String eventType = msg.eventType();
    Long bookingId = payload.get("bookingId").asLong();

    Booking booking = bookingRepository.findById(bookingId).orElse(null);
    if (booking == null) {
      log.warn("Booking {} introuvable, notification ignorée.", bookingId);
      return;
    }

    // Récupération des infos pour le template
    Long userId = booking.getClient().getUser().getId();
    String salonName = booking.getBarber().getSalonName();
    String dateFormatted = booking.getStartAt().format(DATE_FORMATTER);

    // 3. Envoi via PushService (qui utilise les Templates Enum)
    switch (eventType) {
      case "BOOKING_CREATED", "BOOKING_CONFIRMED" ->
        pushService.sendBookingNotification(userId, "CONFIRMED", salonName, dateFormatted);

      case "BOOKING_CANCELLED" ->
        pushService.sendBookingNotification(userId, "CANCELLED", salonName, dateFormatted);

      case "BOOKING_REMINDER_DUE" ->
        pushService.sendBookingNotification(userId, "REMINDER", salonName, dateFormatted);

      case "BOOKING_COMPLETED" ->
        pushService.sendBookingNotification(userId, "COMPLETED", salonName, ""); // Date moins pertinente ici

      default -> log.warn("Type d'événement inconnu : {}", eventType);
    }
  }

  /**
   * LISTENER 2 : Traite les demandes de notifications génériques
   * Déclenché quand on veut envoyer un message libre.
   */
  @KafkaListener(topics = "notification-topic", groupId = "notification-group")
  public void consume(String message) {
    log.info("📩 Message Kafka générique reçu : {}", message);

    try {
      // 1. Désérialisation
      NotificationMessageDto dto = objectMapper.readValue(message, NotificationMessageDto.class);

      // 2. Récupération Utilisateur (pour l'email)
      ApplicationUser user = userRepository.findById(dto.getUserId()).orElse(null);
      if (user == null) {
        log.warn("⚠️ Utilisateur ID {} introuvable.", dto.getUserId());
        return;
      }

      // 3. Canal Email
      try {
        emailService.sendSimpleMessage(user.getEmail(), dto.getTitle(), dto.getBody());
        log.info("✅ Email envoyé à {}", user.getEmail());
      } catch (Exception e) {
        log.error("❌ Erreur envoi Email : {}", e.getMessage());
      }

      // 4. Canal Push (Simplifié grâce au nouveau PushService)
      // On prépare les data supplémentaires
      Map<String, String> data = new HashMap<>();
      data.put("type", dto.getType());
      if (dto.getReferenceId() != null) {
        data.put("bookingId", String.valueOf(dto.getReferenceId()));
        data.put("click_action", "FLUTTER_NOTIFICATION_CLICK");
      }

      // On délègue tout au service : il va chercher le token et envoyer
      pushService.sendToUser(dto.getUserId(), dto.getTitle(), dto.getBody(), data);

    } catch (Exception e) {
      log.error("❌ Erreur critique traitement Kafka", e);
    }
  }

  // Record pour l'Outbox Pattern
  public record OutboxMessage(String id, String aggregateType, String aggregateId,
      String eventType, String payload, String headers, String createdAt) {
  }
}