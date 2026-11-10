package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Config.TenantGuard;
import com.salahtech.BarberShop_Apis.Dtos.SalonClientDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonBookingService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Clients d'un salon (agrégés depuis les réservations). Lecture = tout membre (front-desk) ;
 * un prestataire ne voit que les clients qu'il a servis (données de contact incluses).
 */
@RestController
@RequestMapping("/v1/salons/{salonId}/clients")
@RequiredArgsConstructor
@Tag(name = "Salon Clients", description = "Clients d'un salon")
public class SalonClientsController {

    private final SalonBookingService bookingService;
    private final TenantGuard tenantGuard;
    private final com.salahtech.BarberShop_Apis.repositories.EmployeeRepository employeeRepository;
    private final com.salahtech.BarberShop_Apis.repositories.SalonClientNoteRepository noteRepository;
    private final com.salahtech.BarberShop_Apis.repositories.SalonBookingRepository salonBookingRepository;
    private final com.salahtech.BarberShop_Apis.repositories.QueueEntryRepository queueEntryRepository;
    private final com.salahtech.BarberShop_Apis.repositories.SalonRepository salonRepository;
    private final com.salahtech.BarberShop_Apis.repositories.ClientRepository clientRepository;

    @Operation(summary = "Lister les clients du salon")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalonClientDto>> list(@PathVariable Long salonId,
                                                     @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        Long scope = null;
        if (tenantGuard.isEmployeeOn(user, salonId)) {
            scope = employeeRepository.findByUser_Id(user.getId()).map(e -> e.getId()).orElse(-1L);
        }
        List<SalonClientDto> list = bookingService.listClients(salonId, scope);
        Map<Long, String> notes = new HashMap<>();
        for (Object[] row : noteRepository.notesOfSalon(salonId)) notes.put((Long) row[0], (String) row[1]);
        list.forEach(c -> c.setNote(notes.get(c.getClientId())));
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Écrire / effacer la note privée du salon sur un client")
    @PutMapping("/{clientId}/note")
    @PreAuthorize("isAuthenticated()")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String, String>> setNote(@PathVariable Long salonId,
                                                       @PathVariable Long clientId,
                                                       @RequestBody Map<String, String> body,
                                                       @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        // Le client doit être connu du salon (réservation ou passage en file).
        boolean known = salonBookingRepository.existsBySalon_IdAndClient_Id(salonId, clientId)
                || queueEntryRepository.existsBySalon_IdAndClient_Id(salonId, clientId);
        if (!known) {
            throw new com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException("Client inconnu de ce salon");
        }
        String text = body == null ? null : body.get("note");
        text = text == null ? "" : text.trim();
        if (text.length() > 2000) {
            throw new com.salahtech.BarberShop_Apis.Exceptions.BadRequestException("Note trop longue (2000 caractères max)");
        }
        var existing = noteRepository.findBySalon_IdAndClient_Id(salonId, clientId);
        if (text.isEmpty()) {
            existing.ifPresent(noteRepository::delete);
            return ResponseEntity.ok(Map.of("note", ""));
        }
        var note = existing.orElseGet(() -> com.salahtech.BarberShop_Apis.models.SalonClientNote.builder()
                .salon(salonRepository.getReferenceById(salonId))
                .client(clientRepository.getReferenceById(clientId))
                .build());
        note.setNote(text);
        note.setUpdatedByUserId(user.getId());
        noteRepository.save(note);
        return ResponseEntity.ok(Map.of("note", text));
    }
}
