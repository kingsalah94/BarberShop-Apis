package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.CreateSalonReviewRequest;
import com.salahtech.BarberShop_Apis.Dtos.SalonBookingDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonBookingService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Réservations salon du client connecté. */
@RestController
@RequestMapping("/v1/me/salon-bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "My Salon Bookings", description = "Réservations salon du client connecté")
public class MySalonBookingsController {

    private final SalonBookingService bookingService;

    @Operation(summary = "Mes réservations salon")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalonBookingDto>> mine(@CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(bookingService.listMine(user));
    }

    @Operation(summary = "Annuler ma réservation")
    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonBookingDto> cancel(@PathVariable Long bookingId,
                                                  @CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(bookingService.cancelMine(bookingId, user));
    }

    @Operation(summary = "Laisser un avis sur une réservation terminée")
    @PostMapping("/{bookingId}/review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonBookingDto> review(@PathVariable Long bookingId,
                                                  @Valid @RequestBody CreateSalonReviewRequest req,
                                                  @CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(bookingService.review(bookingId, req, user));
    }
}
