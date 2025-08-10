package com.salahtech.BarberShop_Apis.Web.Controller;


import com.salahtech.BarberShop_Apis.Dtos.AvailabilityDto;
import com.salahtech.BarberShop_Apis.Dtos.TimeSlotDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.AvailabilityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/availabilities")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    // ========= CRUD de base =========

    @PostMapping
    public ResponseEntity<AvailabilityDto> create(@Valid @RequestBody AvailabilityDto dto) {
        AvailabilityDto saved = availabilityService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AvailabilityDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(availabilityService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<AvailabilityDto>> findAll() {
        return ResponseEntity.ok(availabilityService.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        availabilityService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ========= Créneaux disponibles & vérifications =========

    @GetMapping("/slots")
    public ResponseEntity<List<TimeSlotDto>> getAvailableSlots(
            @RequestParam @NotNull Long barberId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @NotNull @Min(1) Integer serviceDurationMinutes
    ) {
        return ResponseEntity.ok(
                availabilityService.getAvailableSlots(barberId, date, serviceDurationMinutes)
        );
    }

    @GetMapping("/slot/check")
    public ResponseEntity<Map<String, Boolean>> isSlotAvailable(
            @RequestParam @NotNull Long barberId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        boolean ok = availabilityService.isSlotAvailable(barberId, start, end);
        return ResponseEntity.ok(Map.of("available", ok));
    }

    // ========= Blocage / Libération de créneau =========

    @PostMapping("/slot/block")
    public ResponseEntity<Void> blockSlot(@Valid @RequestBody BlockRequest req) {
        availabilityService.blockTimeSlot(req.getBarberId(), req.getStartTime(), req.getEndTime(), req.getReason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/slot/release")
    public ResponseEntity<Void> releaseSlot(@Valid @RequestBody ReleaseRequest req) {
        availabilityService.releaseTimeSlot(req.getBarberId(), req.getStartTime(), req.getEndTime());
        return ResponseEntity.noContent().build();
    }

    // ========= Planning hebdomadaire =========

    @GetMapping("/weekly")
    public ResponseEntity<List<AvailabilityDto>> getWeeklyAvailability(
            @RequestParam @NotNull Long barberId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        return ResponseEntity.ok(availabilityService.getWeeklyAvailability(barberId, weekStart));
    }

    @PutMapping("/weekly/{barberId}")
    public ResponseEntity<Void> updateWeeklySchedule(
            @PathVariable Long barberId,
            @Valid @RequestBody List<AvailabilityDto> weeklySchedule
    ) {
        availabilityService.updateWeeklySchedule(barberId, weeklySchedule);
        return ResponseEntity.noContent().build();
    }

    // ========= Disponibilités par jour / prochains créneaux =========

    @GetMapping("/barber/{barberId}/day")
    public ResponseEntity<AvailabilityDto> getBarberAvailabilityForDay(
            @PathVariable Long barberId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(availabilityService.getBarberAvailability(barberId, date));
    }

    @GetMapping("/barber/{barberId}/next")
    public ResponseEntity<List<TimeSlotDto>> getNextAvailableSlots(
            @PathVariable Long barberId,
            @RequestParam(defaultValue = "5") @Min(1) int numberOfSlots
    ) {
        return ResponseEntity.ok(availabilityService.getNextAvailableSlots(barberId, numberOfSlots));
    }

    // ========= Tous les barbiers pour une date =========

    @GetMapping("/day")
    public ResponseEntity<List<AvailabilityDto>> getAllBarbersAvailability(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(availabilityService.getAllBarbersAvailability(date));
    }

    // ========= Statistiques =========
    // Remarque : ton service renvoie une classe interne AvailabilityStatsDTO.
    // On encapsule la réponse en Map pour éviter de référencer directement la classe interne ici.
    // Si tu externalises ce DTO, on pourra le mettre en type de retour fort.
    @GetMapping("/stats")
    public ResponseEntity<?> getAvailabilityStats(
            @RequestParam @NotNull Long barberId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        var stats = availabilityService.getAvailabilityStats(barberId, startDate, endDate);
        // Tentative générique : si la classe interne a bien des getters (Lombok @Data), Jackson la sérialisera.
        // Donc on peut retourner 'stats' tel quel.
        return ResponseEntity.ok(stats);
    }

    // ========= DTOs locaux pour les requêtes =========

    @Data
    public static class BlockRequest {
        @NotNull private Long barberId;
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime startTime;
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime endTime;
        @NotBlank private String reason;
    }

    @Data
    public static class ReleaseRequest {
        @NotNull private Long barberId;
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime startTime;
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime endTime;
    }
}
