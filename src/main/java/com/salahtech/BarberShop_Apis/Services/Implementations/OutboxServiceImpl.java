package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salahtech.BarberShop_Apis.Services.Interfaces.OutboxService;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.outbox.OutboxEvent;
import com.salahtech.BarberShop_Apis.outbox.OutboxStatus;
import com.salahtech.BarberShop_Apis.reppsitories.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OutboxServiceImpl implements OutboxService {

    private final OutboxEventRepository repo;

    // On utilise une instance statique interne pour éviter l'injection de
    // dépendance
    // tout en permettant la conversion Map -> JSON String.
    private static final ObjectMapper JSON_SERIALIZER = new ObjectMapper();

    @Override
    public UUID add(String aggregateType, String aggregateId, String eventType, Map<String, Object> payload,
            Map<String, Object> headers) {
        var id = UUID.randomUUID();
        var now = OffsetDateTime.now();

        String payloadJson;
        String headersJson = null;

        try {
            // Utilisation de notre instance statique locale
            payloadJson = JSON_SERIALIZER.writeValueAsString(payload);
            if (headers != null) {
                headersJson = JSON_SERIALIZER.writeValueAsString(headers);
            }
        } catch (JsonProcessingException e) {
            log.error("Erreur de sérialisation JSON pour l'event {}", eventType, e);
            throw new IllegalStateException("Impossible de sérialiser le payload outbox", e);
        }

        var event = OutboxEvent.builder()
                .id(id)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payloadJson)
                .headers(headersJson)
                .status(OutboxStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        repo.save(event);
        log.debug("Outbox event saved: type={}, id={}", eventType, id);
        return id;
    }

    @Override
    public void enqueueRetry(Booking booking) {
        Map<String, Object> payload = Map.of(
                "bookingId", booking.getId(),
                "barberId", booking.getBarber().getId(),
                "clientId", booking.getClient().getId(),
                "action", "RETRY_NOTIFICATION");

        add("BOOKING",
                booking.getId().toString(),
                "NOTIFICATION_RETRY",
                payload,
                Map.of("attempt", 1) // Astuce pour forcer le type <String, Object>
        );
    }

    @Override
    public UUID enqueueEmail(String to, String subject, String html) {
        // On force le type de la map pour correspondre à la signature de add()
        Map<String, Object> payload = Map.of(
                "to", to,
                "subject", subject,
                "html", html);

        return add("EMAIL", to, "SEND_EMAIL", payload, null);
    }

    // --- MÉTHODES ADMIN (Ajoutées précédemment) ---

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> getEventsByStatus(OutboxStatus status) {
        return repo.findByStatus(status);
    }

    @Override
    public void retryEvent(UUID id) {
        var event = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("OutboxEvent introuvable avec l'id : " + id));

        event.setStatus(OutboxStatus.PENDING);
        event.setNextAttemptAt(OffsetDateTime.now());
        // event.setAttempts(0); // Optionnel

        repo.save(event);
        log.info("Event {} relancé manuellement.", id);
    }

    @Override
    public int retryAllFailed() {
        List<OutboxEvent> failedEvents = repo.findByStatus(OutboxStatus.FAILED);

        if (failedEvents.isEmpty())
            return 0;

        var now = OffsetDateTime.now();
        failedEvents.forEach(event -> {
            event.setStatus(OutboxStatus.PENDING);
            event.setNextAttemptAt(now);
        });

        repo.saveAll(failedEvents);
        log.info("{} events FAILED ont été relancés.", failedEvents.size());
        return failedEvents.size();
    }
}