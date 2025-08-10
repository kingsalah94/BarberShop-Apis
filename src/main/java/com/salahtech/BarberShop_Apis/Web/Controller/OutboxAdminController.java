package com.salahtech.BarberShop_Apis.Web.Controller;


import com.salahtech.BarberShop_Apis.outbox.OutboxEvent;
import com.salahtech.BarberShop_Apis.outbox.OutboxStatus;
import com.salahtech.BarberShop_Apis.reppsitories.OutboxEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/outbox")
@RequiredArgsConstructor
@Validated
@Tag(name = "Outbox Admin", description = "Inspection et relance des messages Outbox")
public class OutboxAdminController {

    private final OutboxEventRepository outboxRepo;

    @Operation(summary = "Lister les événements Outbox par statut (PENDING/SENT/FAILED/SENDING)")
    @GetMapping
    public ResponseEntity<List<OutboxEvent>> listByStatus(@RequestParam(defaultValue = "PENDING") OutboxStatus status) {
        return ResponseEntity.ok(outboxRepo.findByStatusOrderByCreatedAtAsc(status));
    }

    @Operation(summary = "Relancer un message Outbox par id (le remet en PENDING)")
    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retryOne(@PathVariable UUID id) {
        var event = outboxRepo.findById(id).orElse(null);
        if (event == null) return ResponseEntity.notFound().build();
        event.setStatus(OutboxStatus.PENDING);
        event.setNextAttemptAt(OffsetDateTime.now());
        event.setUpdatedAt(OffsetDateTime.now());
        outboxRepo.save(event);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Relancer tous les FAILED (bulk → PENDING)")
    @PostMapping("/retry-failed")
    public ResponseEntity<Integer> retryFailed() {
        var failed = outboxRepo.findByStatusOrderByCreatedAtAsc(OutboxStatus.FAILED);
        failed.forEach(e -> {
            e.setStatus(OutboxStatus.PENDING);
            e.setNextAttemptAt(OffsetDateTime.now());
            e.setUpdatedAt(OffsetDateTime.now());
        });
        outboxRepo.saveAll(failed);
        return ResponseEntity.ok(failed.size());
    }

    @Operation(summary = "Lister les événements prêts à envoyer (status=PENDING et next_attempt_at <= now)")
    @GetMapping("/ready")
    public ResponseEntity<List<OutboxEvent>> listReady(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime now) {
        var when = (now == null) ? OffsetDateTime.now() : now;
        // version read-only (pas lock) pour consultation
        return ResponseEntity.ok(outboxRepo.findReadyForSend(when));
    }
}
