package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Dtos.NotificationDto;
import com.salahtech.BarberShop_Apis.Dtos.NotificationMessageDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.NotificationService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.twilio.rest.api.v2010.account.Application;
import com.twilio.rest.microvisor.v1.App;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
@Validated
@Tag(name = "Notifications", description = "Outils de test et relance de notifications")
public class NotificationsController {

    private final NotificationService notificationService;

    @Operation(summary = "Envoyer une notification manuelle")
    @PostMapping("/custom")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sendCustom(@Valid @RequestBody NotificationDto notification) {
        notificationService.sendCustomNotification(notification);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Générer les notifications manquantes depuis les RDV existants")
    @PostMapping("/generate/{barberId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BARBER')")
    public ResponseEntity<Void> generateNotifications(@PathVariable Long barberId) {
        // Lancer le processus en arrière-plan (Async)
        notificationService.generateNotificationsFromExistingBookings(barberId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Récupérer les notifications de l'utilisateur connecté")
    @GetMapping
    public ResponseEntity<List<NotificationMessageDto>> getMyNotifications(
            @AuthenticationPrincipal ApplicationUser currentUser) {
        // Sécurité : Vérifier si le token est valide et contient un user
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<NotificationMessageDto> notifications = notificationService.getUserNotifications(currentUser.getId());
        return ResponseEntity.ok(notifications);
    }

    // // ✅ AJOUT : Endpoint pour récupérer les notifications du Dashboard
    // @Operation(summary = "Récupérer les notifications de l'utilisateur connecté")
    // @GetMapping
    // public ResponseEntity<List<NotificationDto>> getMyNotifications(
    // @AuthenticationPrincipal ApplicationUser currentUser // Récupère le user via
    // le Token JWT
    // ) {
    // // Appelle une nouvelle méthode dans ton service
    // List<NotificationDto> notifications =
    // notificationService.getUserNotifications(currentUser.getId());
    // return ResponseEntity.ok(notifications);
    // }

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