package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Dtos.ClientDto;
import com.salahtech.BarberShop_Apis.Dtos.BookingDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/clients")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Clients", description = "Gestion des clients")
@SecurityRequirement(name = "bearerAuth")
public class ClientController {

    private final ClientService clientService;

    // ======= CRUD =======

    @Operation(summary = "Créer un client", responses = {
            @ApiResponse(responseCode = "201", description = "Client créé"),
            @ApiResponse(responseCode = "409", description = "Conflit - email/téléphone déjà utilisé")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SALON_OWNER')")
    public ResponseEntity<ClientDto> create(@Valid @RequestBody ClientDto dto) {
        ClientDto saved = clientService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Mettre à jour un client")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENT') and #id == authentication.principal.clientId)")
    public ResponseEntity<ClientDto> update(@PathVariable Long id, @Valid @RequestBody ClientDto dto) {
        dto.setId(id);
        return ResponseEntity.ok(clientService.save(dto));
    }

    @Operation(summary = "Obtenir un client par ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BARBER') or (hasRole('CLIENT') and #id == authentication.principal.clientId)")
    public ResponseEntity<ClientDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @Operation(summary = "Supprimer un client")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ======= RECHERCHE =======

    @Operation(summary = "Rechercher des clients")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BARBER') or hasRole('SALON_OWNER')")
    public ResponseEntity<Page<ClientDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean hasBookings,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(clientService.search(q, hasBookings, createdFrom, createdTo, pageable));
    }

    @Operation(summary = "Obtenir un client par user ID")
    @GetMapping("/by-user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ClientDto> findByUserId(@PathVariable @NotNull Long userId) {
        return ResponseEntity.ok(clientService.findByUserId(userId));
    }

    @Operation(summary = "Obtenir un client par email")
    @GetMapping("/by-email")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BARBER')")
    public ResponseEntity<ClientDto> findByEmail(@RequestParam @Email String email) {
        return ResponseEntity.ok(clientService.findByEmail(email));
    }

    @Operation(summary = "Obtenir un client par téléphone")
    @GetMapping("/by-phone")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BARBER')")
    public ResponseEntity<ClientDto> findByPhone(@RequestParam String phone) {
        return ResponseEntity.ok(clientService.findByPhone(phone));
    }

    // ======= BOOKINGS =======

    @Operation(summary = "Obtenir les réservations d'un client")
    @GetMapping("/{clientId}/bookings")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BARBER') or (hasRole('CLIENT') and #clientId == authentication.principal.clientId)")
    public ResponseEntity<List<BookingDto>> getClientBookings(@PathVariable Long clientId) {
        return ResponseEntity.ok(clientService.getClientBookings(clientId));
    }

    @Operation(summary = "Obtenir les réservations par statut")
    @GetMapping("/{clientId}/bookings/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENT') and #clientId == authentication.principal.clientId)")
    public ResponseEntity<List<BookingDto>> getClientBookingsByStatus(
            @PathVariable Long clientId,
            @PathVariable String status) {
        return ResponseEntity.ok(clientService.getClientBookingsByStatus(clientId, status));
    }

    @Operation(summary = "Obtenir la dernière réservation")
    @GetMapping("/{clientId}/bookings/last")
    public ResponseEntity<BookingDto> getLastBooking(@PathVariable Long clientId) {
        BookingDto lastBooking = clientService.getLastBooking(clientId);
        return lastBooking != null ? ResponseEntity.ok(lastBooking) : ResponseEntity.noContent().build();
    }

    // ======= MISES À JOUR PARTIELLES =======

    @Operation(summary = "Mettre à jour les préférences")
    @PatchMapping("/{clientId}/preferences")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENT') and #clientId == authentication.principal.clientId)")
    public ResponseEntity<ClientDto> updatePreferences(
            @PathVariable Long clientId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(clientService.updatePreferences(clientId, body.get("preferences")));
    }

    @Operation(summary = "Mettre à jour les notes (admin/barber)")
    @PatchMapping("/{clientId}/notes")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BARBER')")
    public ResponseEntity<ClientDto> updateNotes(
            @PathVariable Long clientId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(clientService.updateNotes(clientId, body.get("notes")));
    }

    @Operation(summary = "Mettre à jour les contacts")
    @PatchMapping("/{clientId}/contact")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENT') and #clientId == authentication.principal.clientId)")
    public ResponseEntity<ClientDto> updateContact(
            @PathVariable Long clientId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(clientService.updateContact(
                clientId,
                body.get("email"),
                body.get("phone")));
    }

    // ======= STATISTIQUES =======

    @Operation(summary = "Obtenir les statistiques d'un client")
    @GetMapping("/{clientId}/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BARBER')")
    public ResponseEntity<ClientService.ClientStatsDto> getClientStats(@PathVariable Long clientId) {
        return ResponseEntity.ok(clientService.getClientStats(clientId));
    }

    @Operation(summary = "Obtenir les meilleurs clients")
    @GetMapping("/top")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SALON_OWNER')")
    public ResponseEntity<List<ClientDto>> getTopClients(
            @RequestParam(defaultValue = "10") @Min(1) int limit) {
        return ResponseEntity.ok(clientService.getTopClients(limit));
    }

    @Operation(summary = "Obtenir les clients inactifs")
    @GetMapping("/inactive")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SALON_OWNER')")
    public ResponseEntity<List<ClientDto>> getInactiveClients(
            @RequestParam(defaultValue = "30") @Min(1) int days) {
        return ResponseEntity.ok(clientService.getInactiveClients(days));
    }

    // ======= VALIDATION (suite) =======

    @Operation(summary = "Vérifier si un email existe")
    @GetMapping("/exists/email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> checkEmailExists(@RequestParam @Email String email) {
        return ResponseEntity.ok(Map.of("exists", clientService.existsByEmail(email)));
    }

    @Operation(summary = "Vérifier si un téléphone existe")
    @GetMapping("/exists/phone")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> checkPhoneExists(@RequestParam String phone) {
        return ResponseEntity.ok(Map.of("exists", clientService.existsByPhone(phone)));
    }

    @Operation(summary = "Vérifier si un userId a déjà un profil client")
    @GetMapping("/exists/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> checkUserHasClient(@PathVariable Long userId) {
        return ResponseEntity.ok(Map.of("exists", clientService.existsByUserId(userId)));
    }
}