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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/availabilities")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@Validated
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    // ========= ROUTES SPÉCIFIQUES EN PREMIER =========

    // 1. Routes avec paths fixes AVANT les routes avec {id}

    @GetMapping("/slots")
    public ResponseEntity<List<TimeSlotDto>> getAvailableSlots(
            @RequestParam @NotNull Long barberId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @NotNull @Min(1) Integer serviceDurationMinutes) {
        return ResponseEntity.ok(
                availabilityService.getAvailableSlots(barberId, date, serviceDurationMinutes));
    }

    @GetMapping("/slot/check")
    public ResponseEntity<Map<String, Boolean>> isSlotAvailable(
            @RequestParam @NotNull Long barberId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        boolean ok = availabilityService.isSlotAvailable(barberId, start, end);
        return ResponseEntity.ok(Map.of("available", ok));
    }

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

    // 2. Route weekly avec paramètre de requête (pas de path variable)
    @GetMapping("/weekly")
    public ResponseEntity<List<AvailabilityDto>> getWeeklyAvailability(
            @RequestParam @NotNull Long barberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return ResponseEntity.ok(availabilityService.getWeeklyAvailability(barberId, weekStart));
    }

    @PutMapping("/weekly/{barberId}")
    public ResponseEntity<Void> updateWeeklySchedule(
            @PathVariable Long barberId, // ← corrige ici
            @Valid @RequestBody List<AvailabilityDto> weeklySchedule) {
        availabilityService.updateWeeklySchedule(barberId, weeklySchedule);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/barber/{barberId}/next")
    public ResponseEntity<List<TimeSlotDto>> getNextAvailableSlots(
            @PathVariable Long barberId,
            @RequestParam(defaultValue = "5") @Min(1) int numberOfSlots) {
        return ResponseEntity.ok(availabilityService.getNextAvailableSlots(barberId, numberOfSlots));
    }

    // 4. Route day (tous les barbiers)
    @GetMapping("/day")
    public ResponseEntity<List<AvailabilityDto>> getAllBarbersAvailability(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(availabilityService.getAllBarbersAvailability(date));
    }

    // 5. Route stats
    @GetMapping("/stats")
    public ResponseEntity<?> getAvailabilityStats(
            @RequestParam @NotNull Long barberId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        var stats = availabilityService.getAvailabilityStats(barberId, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    // ========= CRUD DE BASE EN DERNIER (avec {id}) =========

    @PostMapping
    public ResponseEntity<AvailabilityDto> create(@Valid @RequestBody AvailabilityDto dto) {
        var saved = availabilityService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<AvailabilityDto>> findAll() {
        return ResponseEntity.ok(availabilityService.findAll());
    }

    // IMPORTANT : Les routes avec {id} en DERNIER
    @GetMapping("/{id}")
    public ResponseEntity<AvailabilityDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(availabilityService.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        availabilityService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // 3. Routes barber spécifiques
    // (A) NOUVEAU : liste des dispos d’un barbier
    @GetMapping("/barber/{barberId}")
    public ResponseEntity<List<AvailabilityDto>> getByBarber(@PathVariable Long barberId) {
        return ResponseEntity.ok(availabilityService.getByBarber(barberId));
    }

    // (B) EXISTANT "day" : préciser params="date" pour éviter conflit
    @GetMapping(value = "/barber/{barberId}/day", params = "date")
    public ResponseEntity<AvailabilityDto> getBarberAvailabilityForDay(
            @PathVariable Long barberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(availabilityService.getBarberAvailability(barberId, date));
    }

    // (C) OPTIONNEL : fallback sans ?date => aujourd’hui (Niamey)
    @GetMapping("/barber/{barberId}/day")
    public ResponseEntity<AvailabilityDto> getBarberAvailabilityForToday(@PathVariable Long barberId) {
        LocalDate todayNiamey = LocalDate.now(ZoneId.of("Africa/Niamey"));
        return ResponseEntity.ok(availabilityService.getBarberAvailability(barberId, todayNiamey));
    }
    // ========= DTOs requêtes =========

    @Data
    public static class BlockRequest {
        @NotNull
        private Long barberId;
        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime startTime;
        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime endTime;
        @NotBlank
        private String reason;
    }

    @Data
    public static class ReleaseRequest {
        @NotNull
        private Long barberId;
        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime startTime;
        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime endTime;
    }
}