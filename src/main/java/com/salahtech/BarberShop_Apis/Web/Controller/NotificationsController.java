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

}