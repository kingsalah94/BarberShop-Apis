package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.MessageDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.MessageService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class MessageController {

    private final MessageService messageService;

    // Voir toutes mes conversations (Inbox)
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageDto>> getMyConversations(@CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(messageService.getMyConversations(user.getId()));
    }

    // Voir les détails d'une conversation avec un utilisateur spécifique
    @GetMapping("/{otherUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageDto>> getConversation(
            @CurrentUser ApplicationUser user,
            @PathVariable Long otherUserId) {
        // En récupérant la conversation, on marque les messages comme lus
        messageService.markAsRead(user.getId(), otherUserId);
        return ResponseEntity.ok(messageService.getConversation(user.getId(), otherUserId));
    }

    // Envoyer un message
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageDto> sendMessage(
            @CurrentUser ApplicationUser user,
            @RequestBody SendMessageRequest request) {

        return ResponseEntity.ok(messageService.sendMessage(
                user.getId(),
                request.getReceiverId(),
                request.getContent(),
                request.getBookingId()));
    }

    // Compter les non-lus (pour le badge dans le menu)
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getUnreadCount(@CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(messageService.getUnreadCount(user.getId()));
    }

    // Petit DTO interne pour la requête
    @Data
    public static class SendMessageRequest {
        private Long receiverId;
        private String content;
        private Long bookingId; // Optionnel
    }
}
