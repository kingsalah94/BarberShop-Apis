package com.salahtech.BarberShop_Apis.Web.Controller;


import com.salahtech.BarberShop_Apis.Dtos.BookingDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Validated
@Tag(name = "Bookings - Lifecycle", description = "Actions de cycle de vie (confirm/cancel)")
public class BookingLifecycleController {

    private final BookingService bookingService; // Assure-toi d’exposer confirm/cancel dans l’interface

    @Operation(summary = "Confirmer une réservation")
    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<BookingDto> confirm(@PathVariable @NotNull Long bookingId) {
        BookingDto updated = bookingService.confirm(bookingId);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Annuler une réservation")
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable @NotNull Long bookingId) {
        bookingService.cancel(bookingId);
        return ResponseEntity.noContent().build();
    }
}

