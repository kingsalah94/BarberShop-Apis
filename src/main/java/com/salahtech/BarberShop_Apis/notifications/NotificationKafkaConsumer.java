package com.salahtech.BarberShop_Apis.notifications;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salahtech.BarberShop_Apis.Services.Interfaces.NotificationService;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;
import com.salahtech.BarberShop_Apis.reppsitories.ProcessedMessageRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaConsumer {

  private final ObjectMapper mapper;
  private final BookingRepository bookingRepository;
  private final NotificationService notificationService;
  private final ProcessedMessageRepository processedRepo;

  @KafkaListener(topics = "${app.outbox.topic}", groupId = "notifications-service")
  @Transactional
  public void onMessage(OutboxMessage msg) throws Exception {
    UUID messageId = UUID.fromString(msg.id());
    // idempotence: ignore si déjà traité
    try {
      processedRepo.markProcessed(messageId);
    } catch (DataIntegrityViolationException dup) {
      log.info("Duplicate message {} ignored", messageId);
      return;
    }

    // Décoder payload JSON si besoin
    JsonNode payload = mapper.readTree(msg.payload());

    String eventType = msg.eventType();
    Long bookingId = payload.get("bookingId").asLong();

    var booking = bookingRepository.findById(bookingId).orElse(null);
    if (booking == null) {
      log.warn("Booking {} introuvable, skip", bookingId);
      return;
    }

    switch (eventType) {
      case "BOOKING_CREATED", "BOOKING_CONFIRMED" -> notificationService.sendBookingConfirmation(booking);
      case "BOOKING_CANCELLED" -> notificationService.sendBookingCancellation(booking);
      case "BOOKING_REMINDER_DUE" -> notificationService.sendBookingReminder(booking);
      default -> log.warn("Event type inconnu: {}", eventType);
    }
  }

  public record OutboxMessage(String id, String aggregateType, String aggregateId,
                              String eventType, String payload, String headers, String createdAt) {}
}

