package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Dtos.NotificationDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Validated
@Tag(name = "Notifications", description = "Outils de test et relance de notifications")
public class NotificationsController {

    private final NotificationService notificationService;
    // ❌ REMOVED: BookingRepository (Clean Architecture Violation)

    @Operation(summary = "Envoyer une notification manuelle")
    @PostMapping("/custom")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sendCustom(@Valid @RequestBody NotificationDto notification) {
        notificationService.sendCustomNotification(notification);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Renvoyer la confirmation (Trigger)")
    @PostMapping("/booking/{bookingId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resendConfirmation(@PathVariable @NotNull Long bookingId) {
        // Le service doit avoir une méthode : void resendBookingConfirmation(Long id);
        // Si elle n'existe pas, il faut la créer dans NotificationService
        notificationService.resendBookingConfirmation(bookingId); 
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Envoyer un rappel (Trigger)")
    @PostMapping("/booking/{bookingId}/reminder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sendReminder(@PathVariable @NotNull Long bookingId) {
        notificationService.resendBookingReminder(bookingId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Notifier l'annulation (Trigger)")
    @PostMapping("/booking/{bookingId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sendCancellation(@PathVariable @NotNull Long bookingId) {
        notificationService.resendBookingCancellation(bookingId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}