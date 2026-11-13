package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Dtos.NotificationDto;
import com.salahtech.BarberShop_Apis.Dtos.NotificationMessageDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.NotificationService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.repositories.NotificationRepository;
import org.springframework.transaction.annotation.Transactional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

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
    private final NotificationRepository notificationRepository;

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

    @Operation(summary = "Nombre de notifications non lues")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal ApplicationUser currentUser) {
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(Map.of("count", notificationRepository.countByUserIdAndReadFalse(currentUser.getId())));
    }

    @Operation(summary = "Marquer une notification comme lue")
    @PutMapping("/{id}/read")
    @Transactional
    public ResponseEntity<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal ApplicationUser currentUser) {
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return notificationRepository.findByIdAndUserId(id, currentUser.getId())
                .map(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Tout marquer comme lu")
    @PutMapping("/read-all")
    @Transactional
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal ApplicationUser currentUser) {
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        notificationRepository.markAllRead(currentUser.getId());
        return ResponseEntity.noContent().build();
    }

}
