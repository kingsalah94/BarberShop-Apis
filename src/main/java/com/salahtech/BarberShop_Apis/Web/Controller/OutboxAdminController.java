package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Services.Interfaces.OutboxService;
import com.salahtech.BarberShop_Apis.outbox.OutboxEvent;
import com.salahtech.BarberShop_Apis.outbox.OutboxStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/outbox")
@RequiredArgsConstructor
@Tag(name = "Outbox Admin", description = "Gestion des messages asynchrones (Kafka)")
@PreAuthorize("hasRole('ADMIN')") // Sécurisé globalement pour ce contrôleur
public class OutboxAdminController {

    private final OutboxService outboxService;

    @Operation(summary = "Lister les événements par statut")
    @GetMapping
    public ResponseEntity<List<OutboxEvent>> listByStatus(@RequestParam(defaultValue = "PENDING") OutboxStatus status) {
        return ResponseEntity.ok(outboxService.getEventsByStatus(status));
    }

    @Operation(summary = "Relancer un message spécifique")
    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retryOne(@PathVariable UUID id) {
        outboxService.retryEvent(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Relancer tous les messages en échec")
    @PostMapping("/retry-failed")
    public ResponseEntity<Integer> retryFailed() {
        return ResponseEntity.ok(outboxService.retryAllFailed());
    }
}