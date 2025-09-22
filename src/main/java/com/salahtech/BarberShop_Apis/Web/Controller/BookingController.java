package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Dtos.BookingDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Services.Interfaces.BookingService;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Validated
@Tag(name = "Bookings", description = "Gestion des réservations")
public class BookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    // ======= CRUD =======

    @Operation(summary = "Créer une réservation", responses = @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = BookingDto.class))))
    @PostMapping
    public ResponseEntity<BookingDto> create(@Valid @RequestBody BookingDto dto) {
        BookingDto saved = bookingService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Mettre à jour une réservation par ID")
    @PutMapping("/{id}")
    public ResponseEntity<BookingDto> update(@PathVariable Long id, @Valid @RequestBody BookingDto dto) {
        dto.setId(id);
        return ResponseEntity.ok(bookingService.save(dto));
    }

    @Operation(summary = "Trouver une réservation par ID")
    @GetMapping("/{id}")
    public ResponseEntity<BookingDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.findById(id));
    }

    @Operation(summary = "Supprimer une réservation par ID", responses = @ApiResponse(responseCode = "204", description = "Supprimé"))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ======= LISTE =======

    // ======= RECHERCHES =======

    @Operation(summary = "Réservations d'un utilisateur")
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<BookingDto>> findByUserId(@PathVariable @NotNull Long userId) {
        return ResponseEntity.ok(bookingService.findByClientId(userId));
    }

    @Operation(summary = "Réservations d'un barbier")
    @GetMapping("/by-barber/{barberId}")
    public ResponseEntity<List<BookingDto>> findByBarberId(@PathVariable @NotNull Long barberId) {
        return ResponseEntity.ok(bookingService.findByBarberId(barberId));
    }

    @Operation(summary = "Réservations d'un service (prestation)")
    @GetMapping("/by-service/{serviceId}")
    public ResponseEntity<List<BookingDto>> findByServiceId(@PathVariable @NotNull Long serviceId) {
        return ResponseEntity.ok(bookingService.findByServiceId(serviceId));
    }

    @GetMapping(value = "", params = { "page", "size" })
    public Page<BookingDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long barberId,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable) {
        BookingStatus st = (status == null || status.isBlank())
                ? null
                : BookingStatus.valueOf(status.toUpperCase());

        // bornes par défaut : [1970-01-01, 9999-12-31)
        LocalDateTime from = (dateFrom != null)
                ? dateFrom.atStartOfDay()
                : LocalDateTime.of(1970, 1, 1, 0, 0);

        // borne haute EXCLUSIVE : +1 jour à minuit
        LocalDateTime to = (dateTo != null)
                ? dateTo.plusDays(1).atStartOfDay()
                : LocalDateTime.of(9999, 12, 31, 23, 59, 59);

        String qEff = (q == null || q.isBlank()) ? null : q.trim();

        return bookingRepository.searchWithFilters(qEff, st, barberId, clientId, from, to, pageable)
                .map(BookingDto::fromEntity);
    }

    // (optionnel) si vous tenez à findAll(), mettez un autre path pour éviter le
    // conflit :
    @GetMapping("/all")
    public List<BookingDto> findAll() {
        return bookingRepository.findAll()
                .stream().map(BookingDto::fromEntity).toList();
    }

    @Operation(summary = "Réservations par statut", description = "Statuts possibles: PENDING, CONFIRMED, COMPLETED, CANCELLED (selon ton enum)")
    @GetMapping("/status")
    public ResponseEntity<List<BookingDto>> findByStatus(
            @RequestParam("value") @NotBlank @Parameter(description = "Statut (enum BookingStatus)") String status) {
        return ResponseEntity.ok(bookingService.findByStatus(status));
    }

    @Operation(summary = "Réservations d'un barbier sur une période (conflits)")
    @GetMapping("/barber/{barberId}/between")
    public ResponseEntity<List<BookingDto>> findByBarberBetweenDates(
            @PathVariable @NotNull Long barberId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "Début ISO, ex: 2025-08-09T09:00:00") LocalDateTime start,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "Fin ISO, ex: 2025-08-09T18:00:00") LocalDateTime end) {
        return ResponseEntity.ok(bookingService.findByBarberIdBetweenDates(barberId, start, end));
    }

    @Operation(summary = "Compter les réservations complétées d'un barbier")
    @GetMapping("/barber/{barberId}/completed/count")
    public ResponseEntity<Map<String, Long>> countCompletedByBarber(@PathVariable @NotNull Long barberId) {
        Long count = bookingService.countCompletedByBarber(barberId);
        return ResponseEntity.ok(Map.of("barberId", barberId, "completedCount", count));
    }
}
