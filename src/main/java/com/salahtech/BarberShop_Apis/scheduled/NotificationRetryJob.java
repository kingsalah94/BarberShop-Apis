package com.salahtech.BarberShop_Apis.scheduled;

import com.salahtech.BarberShop_Apis.Services.OutboxService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.NotificationService;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.outbox.OutboxEvent;
import com.salahtech.BarberShop_Apis.outbox.OutboxStatus;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;
import com.salahtech.BarberShop_Apis.reppsitories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationRetryJob {

    private final OutboxEventRepository outboxRepo;
    private final NotificationService notificationService;
    private final BookingRepository bookingRepository;

    @Scheduled(fixedDelay = 60000) // Toutes les minutes
    public void processRetryNotifications() {
        List<OutboxEvent> retryEvents = outboxRepo.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)
                .stream()
                .filter(e -> "NOTIFICATION_RETRY".equals(e.getEventType()))
                .limit(10)
                .toList();

        for (OutboxEvent event : retryEvents) {
            try {
                // Extraire l'ID de booking du payload
                Long bookingId = extractBookingId(event.getPayload());
                Booking booking = bookingRepository.findById(bookingId).orElse(null);

                if (booking != null) {
                    // Réessayer l'envoi
                    notificationService.sendBookingConfirmation(booking).get();

                    // Marquer comme envoyé
                    event.setStatus(OutboxStatus.SENT);
                    event.setProcessedAt(OffsetDateTime.now());
                }

            } catch (Exception e) {
                log.error("Échec du retry pour event {}: {}", event.getId(), e.getMessage());

                // Incrémenter le compteur d'essais
                event.setAttempts(event.getAttempts() + 1);

                if (event.getAttempts() >= 3) {
                    event.setStatus(OutboxStatus.FAILED);
                    event.setError(e.getMessage());
                }
            } finally {
                outboxRepo.save(event);
            }
        }
    }

    private Long extractBookingId(String payloadJson) {
        // Implémenter la logique pour extraire l'ID de booking du JSON
        // Par exemple, en utilisant une bibliothèque comme Jackson ou Gson
        // Ici, on suppose que le payload est un simple JSON avec un champ "bookingId"
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(payloadJson);
            return node.get("bookingId").asLong();
        } catch (Exception e) {
            log.error("Erreur lors de l'extraction de bookingId du payload: {}", e.getMessage());
            return null;
        }
    }
}
