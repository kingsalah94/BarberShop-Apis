package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Config.TenantGuard;
import com.salahtech.BarberShop_Apis.Dtos.CheckInRequest;
import com.salahtech.BarberShop_Apis.Dtos.QueueEntryDto;
import com.salahtech.BarberShop_Apis.Dtos.QueuePositionDto;
import com.salahtech.BarberShop_Apis.Enums.PaymentMethod;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.QueueService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.repositories.QueueEntryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.salahtech.BarberShop_Apis.Services.ReceiptPdfService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * File d'attente.
 *
 * <p><b>Isolation multi-tenant</b> : toute opération de gestion vérifie que
 * l'appelant fait partie du personnel du salon concerné
 * ({@link TenantGuard#assertSalonStaff}). Un client ne peut agir que sur
 * sa propre entrée (check-in pour lui-même, position, annulation).</p>
 */
@RestController
@RequestMapping("/v1/queue")
@RequiredArgsConstructor
@Tag(name = "Queue", description = "Gestion de la file d'attente du salon")
public class QueueController {

    private static final String STAFF_ROLES =
            "hasAnyRole('ADMIN', 'SALON_OWNER', 'BARBER', 'MANAGER', 'RECEPTIONIST')";

    private final QueueService queueService;
    private final ReceiptPdfService receiptPdfService;
    private final TenantGuard tenantGuard;
    private final QueueEntryRepository queueEntryRepository;

    // ===================== Helpers d'accès =====================

    private Long salonOfEntry(Long entryId) {
        return queueEntryRepository.findSalonIdById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrée file introuvable: " + entryId));
    }

    /** Personnel du salon de l'entrée (ADMIN : bypass). */
    private void assertStaffOfEntry(ApplicationUser user, Long entryId) {
        tenantGuard.assertSalonStaff(user, salonOfEntry(entryId));
    }

    /** Personnel du salon de l'entrée OU client propriétaire de l'entrée. */
    private void assertStaffOrOwnerOfEntry(ApplicationUser user, Long entryId) {
        Long salonId = salonOfEntry(entryId);
        if (tenantGuard.isSalonStaff(user, salonId)) return;
        Long myClientId = myClientId(user);
        Long entryClientId = queueEntryRepository.findClientIdById(entryId).orElse(null);
        if (myClientId == null || !Objects.equals(myClientId, entryClientId)) {
            throw new AccessDeniedException("Accès refusé : cette entrée ne vous appartient pas");
        }
    }

    private static Long myClientId(ApplicationUser user) {
        return (user != null && user.getClient() != null) ? user.getClient().getId() : null;
    }

    // ===================== Endpoints =====================

    @Operation(summary = "Enregistrer un client dans la file (check-in)")
    @PostMapping("/check-in")
    public ResponseEntity<QueueEntryDto> checkIn(@Valid @RequestBody CheckInRequest req,
                                                 @CurrentUser ApplicationUser user) {
        if (!tenantGuard.isSalonStaff(user, req.getSalonId())) {
            // Auto check-in d'un client : uniquement pour lui-même.
            Long clientId = myClientId(user);
            if (clientId == null) {
                throw new AccessDeniedException("Check-in réservé au personnel du salon ou à un client");
            }
            req.setClientId(clientId);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(queueService.checkIn(req));
    }

    @Operation(summary = "Lister la file active (en attente, notifié, en service)")
    @GetMapping("/salon/{salonId}")
    public ResponseEntity<List<QueueEntryDto>> getActive(@PathVariable Long salonId,
                                                         @CurrentUser ApplicationUser user) {
        tenantGuard.assertSalonStaff(user, salonId);
        return ResponseEntity.ok(queueService.getActiveQueue(salonId));
    }

    @Operation(summary = "Lister uniquement les clients en attente")
    @GetMapping("/salon/{salonId}/waiting")
    public ResponseEntity<List<QueueEntryDto>> getWaiting(@PathVariable Long salonId,
                                                          @CurrentUser ApplicationUser user) {
        tenantGuard.assertSalonStaff(user, salonId);
        return ResponseEntity.ok(queueService.getWaitingQueue(salonId));
    }

    @Operation(summary = "Statistiques de la file (par statut + total du jour)")
    @GetMapping("/salon/{salonId}/stats")
    @PreAuthorize(STAFF_ROLES)
    public ResponseEntity<Map<String, Long>> getStats(@PathVariable Long salonId,
                                                      @CurrentUser ApplicationUser user) {
        tenantGuard.assertSalonStaff(user, salonId);
        return ResponseEntity.ok(queueService.getStats(salonId));
    }

    @Operation(summary = "Prestations terminées du jour (caisse / rattrapage / reçus)")
    @GetMapping("/salon/{salonId}/completed")
    @PreAuthorize(STAFF_ROLES)
    public ResponseEntity<List<QueueEntryDto>> getCompleted(
            @PathVariable Long salonId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @CurrentUser ApplicationUser user) {
        tenantGuard.assertSalonStaff(user, salonId);
        return ResponseEntity.ok(queueService.getCompletedForDay(salonId, date));
    }

    @Operation(summary = "Reçu PDF d'une prestation")
    @GetMapping("/{queueEntryId}/receipt")
    @PreAuthorize(STAFF_ROLES)
    public ResponseEntity<byte[]> receipt(@PathVariable Long queueEntryId,
                                          @CurrentUser ApplicationUser user) {
        assertStaffOfEntry(user, queueEntryId);
        byte[] pdf = receiptPdfService.buildForEntry(queueEntryId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"recu-" + queueEntryId + ".pdf\"")
                .body(pdf);
    }

    @Operation(summary = "Mes tickets actifs, tous salons confondus (client connecté)")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<QueueEntryDto>> myEntries(@CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(queueService.myActiveEntries(myClientId(user)));
    }

    @Operation(summary = "Ma place dans la file d'un salon (client connecté)")
    @GetMapping("/salon/{salonId}/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QueuePositionDto> myPosition(@PathVariable Long salonId,
                                                       @CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(queueService.myPosition(salonId, myClientId(user)));
    }

    @Operation(summary = "Position et temps d'attente pour un ticket")
    @GetMapping("/{queueEntryId}/position")
    public ResponseEntity<QueuePositionDto> getPosition(@PathVariable Long queueEntryId,
                                                        @CurrentUser ApplicationUser user) {
        assertStaffOrOwnerOfEntry(user, queueEntryId);
        return ResponseEntity.ok(queueService.getPosition(queueEntryId));
    }

    @Operation(summary = "Appeler le prochain client (FIFO) sur un poste")
    @PostMapping("/salon/{salonId}/call-next")
    @PreAuthorize(STAFF_ROLES)
    public ResponseEntity<QueueEntryDto> callNext(@PathVariable Long salonId,
                                                  @RequestParam Long workstationId,
                                                  @CurrentUser ApplicationUser user) {
        tenantGuard.assertSalonStaff(user, salonId);
        return ResponseEntity.ok(queueService.callNext(salonId, workstationId));
    }

    @Operation(summary = "Appeler un client spécifique sur un poste")
    @PostMapping("/{queueEntryId}/call")
    @PreAuthorize(STAFF_ROLES)
    public ResponseEntity<QueueEntryDto> callSpecific(@PathVariable Long queueEntryId,
                                                      @RequestParam Long workstationId,
                                                      @CurrentUser ApplicationUser user) {
        assertStaffOfEntry(user, queueEntryId);
        return ResponseEntity.ok(queueService.callSpecific(queueEntryId, workstationId));
    }

    @Operation(summary = "Démarrer le service (le client s'est installé)")
    @PatchMapping("/{queueEntryId}/start")
    @PreAuthorize(STAFF_ROLES)
    public ResponseEntity<QueueEntryDto> start(@PathVariable Long queueEntryId,
                                               @CurrentUser ApplicationUser user) {
        assertStaffOfEntry(user, queueEntryId);
        return ResponseEntity.ok(queueService.startService(queueEntryId));
    }

    @Operation(summary = "Marquer comme terminé")
    @PatchMapping("/{queueEntryId}/complete")
    @PreAuthorize(STAFF_ROLES)
    public ResponseEntity<QueueEntryDto> complete(@PathVariable Long queueEntryId,
                                                  @CurrentUser ApplicationUser user) {
        assertStaffOfEntry(user, queueEntryId);
        return ResponseEntity.ok(queueService.complete(queueEntryId));
    }

    @Operation(summary = "Encaisser le paiement d'une prestation")
    @PatchMapping("/{queueEntryId}/pay")
    @PreAuthorize(STAFF_ROLES)
    public ResponseEntity<QueueEntryDto> pay(@PathVariable Long queueEntryId,
                                             @RequestParam PaymentMethod method,
                                             @RequestParam(required = false) BigDecimal amount,
                                             @RequestParam(defaultValue = "false") boolean useReward,
                                             @CurrentUser ApplicationUser user) {
        assertStaffOfEntry(user, queueEntryId);
        return ResponseEntity.ok(queueService.pay(queueEntryId, method, amount, useReward));
    }

    @Operation(summary = "Annuler une entrée de file")
    @PatchMapping("/{queueEntryId}/cancel")
    public ResponseEntity<QueueEntryDto> cancel(@PathVariable Long queueEntryId,
                                                @RequestParam(required = false) String reason,
                                                @CurrentUser ApplicationUser user) {
        assertStaffOrOwnerOfEntry(user, queueEntryId);
        return ResponseEntity.ok(queueService.cancel(queueEntryId, reason));
    }

    @Operation(summary = "Marquer comme absent (no-show)")
    @PatchMapping("/{queueEntryId}/no-show")
    @PreAuthorize(STAFF_ROLES)
    public ResponseEntity<QueueEntryDto> noShow(@PathVariable Long queueEntryId,
                                                @CurrentUser ApplicationUser user) {
        assertStaffOfEntry(user, queueEntryId);
        return ResponseEntity.ok(queueService.markNoShow(queueEntryId));
    }
}
