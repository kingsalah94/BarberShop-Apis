package com.salahtech.BarberShop_Apis.Web.Controller;



import com.salahtech.BarberShop_Apis.Dtos.NotificationDto;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.NotificationService;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
@Tag(name = "Notifications", description = "Test/relance des notifications (admin/ops)")
public class NotificationsController {

    private final NotificationService notificationService;
    private final BookingRepository bookingRepository;

    @Operation(summary = "Envoyer une notification personnalisée (email/sms/push)")
    @PostMapping("/custom")
    public ResponseEntity<Void> sendCustom(@Valid @RequestBody NotificationDto notification) {
        // Asynchrone -> 202 Accepted
        notificationService.sendCustomNotification(notification);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Renvoyer la confirmation d'une réservation")
    @PostMapping("/booking/{bookingId}/confirm")
    public ResponseEntity<Void> resendConfirmation(@PathVariable @NotNull Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        notificationService.sendBookingConfirmation(booking);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Envoyer un rappel pour une réservation")
    @PostMapping("/booking/{bookingId}/reminder")
    public ResponseEntity<Void> sendReminder(@PathVariable @NotNull Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        notificationService.sendBookingReminder(booking);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Notifier l'annulation d'une réservation")
    @PostMapping("/booking/{bookingId}/cancel")
    public ResponseEntity<Void> sendCancellation(@PathVariable @NotNull Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        notificationService.sendBookingCancellation(booking);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}

