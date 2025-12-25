package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.*;
import com.salahtech.BarberShop_Apis.Services.Interfaces.AvailabilityService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/availabilities")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Validated
@Tag(name = "Availability", description = "Gestion du planning et des créneaux")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    // ==========================================
    // 🔍 LECTURE PUBLIQUE (Slots, Check, Day)
    // ==========================================

    @Operation(summary = "Trouver les créneaux libres (pour un RDV)")
    @GetMapping("/slots")
    public ResponseEntity<List<TimeSlotDto>> getAvailableSlots(
            @RequestParam @NotNull Long barberId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @NotNull @Min(1) Integer serviceDurationMinutes) {
        return ResponseEntity.ok(
                availabilityService.getAvailableSlots(barberId, date, serviceDurationMinutes));
    }

    @Operation(summary = "Vérifier si un créneau précis est libre")
    @GetMapping("/slot/check")
    public ResponseEntity<Map<String, Boolean>> isSlotAvailable(
            @RequestParam @NotNull Long barberId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(Map.of("available", availabilityService.isSlotAvailable(barberId, start, end)));
    }

    @Operation(summary = "Disponibilité de tous les barbiers pour un jour")
    @GetMapping("/day")
    public ResponseEntity<List<AvailabilityDto>> getAllBarbersAvailability(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(availabilityService.getAllBarbersAvailability(date));
    }

    @GetMapping("/barber/{barberId}/next")
    public ResponseEntity<List<TimeSlotDto>> getNextAvailableSlots(
            @PathVariable Long barberId,
            @RequestParam(defaultValue = "5") @Min(1) int numberOfSlots) {
        return ResponseEntity.ok(availabilityService.getNextAvailableSlots(barberId, numberOfSlots));
    }

    // ==========================================
    // 📅 GESTION SEMAINE (Barbier / Admin)
    // ==========================================

    @Operation(summary = "Voir la semaine type (Barbier connecté ou public)")
    @GetMapping("/weekly")
    public ResponseEntity<List<AvailabilityDto>> getWeeklyAvailability(
            @RequestParam(required = false) Long barberId,
            @CurrentUser ApplicationUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        // Si barberId est null, on assume que c'est le barbier connecté qui demande son
        // planning
        Long targetId = (barberId != null) ? barberId : (user != null ? user.getId() : null);

        if (targetId == null)
            return ResponseEntity.badRequest().build();

        return ResponseEntity.ok(availabilityService.getWeeklyAvailability(targetId, weekStart));
    }

    @Operation(summary = "Mettre à jour MA semaine type")
    @PutMapping("/weekly")
    @PreAuthorize("hasAnyRole('ADMIN', 'BARBER', 'SALON_OWNER')")
    public ResponseEntity<Void> updateWeeklySchedule(
            @CurrentUser ApplicationUser user,
            @Valid @RequestBody List<AvailabilityDto> weeklySchedule) {
        // Sécurité : on utilise user.getId()
        availabilityService.updateWeeklySchedule(user.getId(), weeklySchedule);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 🛑 BLOCAGE RAPIDE / EXCEPTIONS
    // ==========================================

    @Operation(summary = "Bloquer un créneau (Urgence)")
    @PostMapping("/slot/block")
    @PreAuthorize("hasAnyRole('ADMIN', 'BARBER', 'SALON_OWNER')")
    public ResponseEntity<Void> blockSlot(
            @CurrentUser ApplicationUser user,
            @Valid @RequestBody BlockSlotRequest req) {
        availabilityService.blockTimeSlot(user.getId(), req.getStartTime(), req.getEndTime(), req.getReason());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Libérer un créneau bloqué")
    @PostMapping("/slot/release")
    @PreAuthorize("hasAnyRole('ADMIN', 'BARBER', 'SALON_OWNER')")
    public ResponseEntity<Void> releaseSlot(
            @CurrentUser ApplicationUser user,
            @Valid @RequestBody ReleaseSlotRequest req) {
        availabilityService.releaseTimeSlot(user.getId(), req.getStartTime(), req.getEndTime());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Créer une exception (Congé, Jour férié)")
    @PostMapping("/exceptions")
    @PreAuthorize("hasAnyRole('ADMIN', 'BARBER', 'SALON_OWNER')")
    public ResponseEntity<AvailabilityDto> createException(
            @CurrentUser ApplicationUser user,
            @Valid @RequestBody CreateExceptionRequest req) {

        List<ExceptionTimeSlotDto> slots = req.getCustomTimeSlots() != null
                ? req.getCustomTimeSlots().stream()
                        .map(ts -> new ExceptionTimeSlotDto(ts.getStartAt(), ts.getEndAt()))
                        .collect(Collectors.toList())
                : null;

        return ResponseEntity.status(HttpStatus.CREATED).body(
                availabilityService.createException(user.getId(), req.getExceptionDate(), req.getExceptionType(),
                        req.getReason(), slots));
    }

    // ==========================================
    // 📊 STATS
    // ==========================================

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAvailabilityStats(
            @CurrentUser ApplicationUser user,
            @RequestParam(required = false) Long barberId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Si Admin, peut voir n'importe qui. Sinon, voit ses propres stats.
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));
        Long targetId = isAdmin && barberId != null ? barberId : user.getId();

        return ResponseEntity.ok(availabilityService.getAvailabilityStats(targetId, startDate, endDate));
    }

    // ==========================================
    // CRUD BASE & EXCEPTIONS (Lecture)
    // ==========================================

    @GetMapping("/exceptions/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'BARBER', 'SALON_OWNER')")
    public ResponseEntity<List<AvailabilityDto>> getMyExceptions(
            @CurrentUser ApplicationUser user,
            @RequestParam(required = false) Boolean upcoming) {
        if (Boolean.TRUE.equals(upcoming)) {
            return ResponseEntity.ok(availabilityService.getUpcomingExceptions(user.getId()));
        }
        return ResponseEntity.ok(availabilityService.getAllExceptions(user.getId()));
    }

    // ... Les autres méthodes GET/DELETE by ID restent inchangées, elles sont
    // standard ...
    @DeleteMapping("/exceptions/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteException(@PathVariable Long id) {
        availabilityService.deleteException(id);
        return ResponseEntity.noContent().build();
    }
}