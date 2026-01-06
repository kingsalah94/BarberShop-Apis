package com.salahtech.BarberShop_Apis.Web.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.*;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ClientService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/clients")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Clients", description = "Gestion des clients et favoris")
public class ClientController {

    private final ClientService clientService;

    // ==========================================
    // 👤 GESTION MON PROFIL (Client Connecté)
    // ==========================================

    @Operation(summary = "Mon Profil Client")
    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientProfileDto> getMyProfile(@CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(clientService.getClientProfile(user.getClient().getId()));
    }

    @Operation(summary = "Mettre à jour mon profil")
    @PutMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientProfileDto> updateMyProfile(
            @CurrentUser ApplicationUser user,
            @Valid @RequestBody UpdateClientDto updateDto) {
        return ResponseEntity.ok(clientService.updateClientProfile(user.getClient().getId(), updateDto));
    }

    // ✅ 1. CREATE avec Image (POST)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClientDto> createClient(
            @RequestPart("user") String clientJson, // On reçoit le JSON en String et on le parse, ou directement en Dto
                                                    // si configuré
            @RequestPart(value = "file", required = false) MultipartFile file) throws JsonProcessingException {
        // Conversion manuelle si besoin, ou laisser Spring le faire si ObjectMapper est
        // bien configuré
        ObjectMapper mapper = new ObjectMapper();
        ClientDto clientDto = mapper.readValue(clientJson, ClientDto.class);

        return ResponseEntity.ok(clientService.create(clientDto, file));
    }

    // ✅ 2. UPDATE avec Image (PUT /{id}) -> Corrige l'erreur 405
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClientDto> updateClient(
            @PathVariable Long id,
            @RequestPart("user") String clientJson,
            @RequestPart(value = "file", required = false) MultipartFile file) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ClientDto clientDto = mapper.readValue(clientJson, ClientDto.class);

        return ResponseEntity.ok(clientService.update(id, clientDto, file));
    }

    @Operation(summary = "Mes Statistiques (Dashboard)")
    @GetMapping("/me/dashboard")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public ResponseEntity<ClientDashboardStatsDto> getMyDashboard(@CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(clientService.getDashboardStats(user.getClient().getId()));
    }

    // ==========================================
    // ⭐ FAVORIS (Client Connecté)
    // ==========================================

    @Operation(summary = "Mes Barbiers Favoris")
    @GetMapping("/me/favorites")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public ResponseEntity<List<BarberDto>> getMyFavorites(@CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(clientService.getFavoriteBarbers(user.getClient().getId()));
    }

    @Operation(summary = "Ajouter un favori")
    @PostMapping("/me/favorites/{barberId}")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public ResponseEntity<Void> addFavorite(@CurrentUser ApplicationUser user, @PathVariable Long barberId) {
        clientService.addFavorite(user.getClient().getId(), barberId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Retirer un favori")
    @DeleteMapping("/me/favorites/{barberId}")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public ResponseEntity<Void> removeFavorite(@CurrentUser ApplicationUser user, @PathVariable Long barberId) {
        clientService.removeFavorite(user.getClient().getId(), barberId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Est-ce un favori ?")
    @GetMapping("/me/favorites/{barberId}/check")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(@CurrentUser ApplicationUser user,
            @PathVariable Long barberId) {
        boolean isFav = clientService.isFavorite(user.getClient().getId(), barberId);
        return ResponseEntity.ok(Map.of("isFavorite", isFav));
    }

    // ==========================================
    // 🛠️ ADMIN ONLY (Gestion Clients)
    // ==========================================

    @Operation(summary = "Lister tous les clients (Admin)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ClientDto>> searchClients(
            @RequestParam(required = false) String q,
            Pageable pageable) {
        // J'ai simplifié la méthode search pour l'exemple, adaptez selon votre service
        return ResponseEntity.ok(clientService.search(q, null, null, null, pageable));
    }

    @Operation(summary = "Détail d'un client (Admin/Barbier)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BARBER')")
    public ResponseEntity<ClientDto> getClientById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @Operation(summary = "Supprimer un client (Admin)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 📅 BOOKINGS CLIENT (Admin/Barbier view)
    // ==========================================
    // Note: Le client utilise /v1/bookings/my-bookings, pas ça.

    @Operation(summary = "Historique RDV d'un client (Admin/Barbier)")
    @GetMapping("/{clientId}/bookings")
    @PreAuthorize("hasAnyRole('ADMIN', 'BARBER')")
    public ResponseEntity<List<BookingDto>> getClientBookings(@PathVariable Long clientId) {
        return ResponseEntity.ok(clientService.getClientBookings(clientId));
    }

    @Operation(summary = "Ajouter une note privée sur un client (Barbier)")
    @PatchMapping("/{clientId}/notes")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<ClientDto> updateClientNotes(@PathVariable Long clientId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(clientService.updateNotes(clientId, body.get("notes")));
    }
}