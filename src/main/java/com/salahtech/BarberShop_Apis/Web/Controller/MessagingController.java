package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.ContactDto;
import com.salahtech.BarberShop_Apis.Dtos.ConversationDto;
import com.salahtech.BarberShop_Apis.Dtos.ConversationListResponse;
import com.salahtech.BarberShop_Apis.Dtos.ConversationMessageDto;
import com.salahtech.BarberShop_Apis.Dtos.CreateConversationRequest;
import com.salahtech.BarberShop_Apis.Dtos.MarkReadRequest;
import com.salahtech.BarberShop_Apis.Dtos.MessagePageResponse;
import com.salahtech.BarberShop_Apis.Dtos.SendChatMessageRequest;
import com.salahtech.BarberShop_Apis.Services.Implementations.FileStorageService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ConversationService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/messaging")
@RequiredArgsConstructor
@Tag(name = "Messaging", description = "Messagerie à conversations (temps réel)")
public class MessagingController {

    private final ConversationService conversationService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "Rechercher des utilisateurs à contacter")
    @GetMapping("/contacts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ContactDto>> contacts(
            @CurrentUser ApplicationUser user,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(conversationService.searchContacts(user.getId(), q, limit));
    }

    @Operation(summary = "Lister mes conversations")
    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversationListResponse> listConversations(
            @CurrentUser ApplicationUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(conversationService.listConversations(user.getId(), page, size));
    }

    @Operation(summary = "Créer (ou retrouver) une conversation")
    @PostMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversationDto> createConversation(
            @CurrentUser ApplicationUser user,
            @Valid @RequestBody CreateConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversationService.createConversation(user.getId(), request));
    }

    @Operation(summary = "Archiver une conversation")
    @PutMapping("/conversations/{id}/archive")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> archive(@CurrentUser ApplicationUser user, @PathVariable Long id) {
        conversationService.archive(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Messages d'une conversation (paginé)")
    @GetMapping("/conversations/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessagePageResponse> listMessages(
            @CurrentUser ApplicationUser user,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(conversationService.listMessages(user.getId(), id, page, size));
    }

    @Operation(summary = "Envoyer un message")
    @PostMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversationMessageDto> sendMessage(
            @CurrentUser ApplicationUser user,
            @Valid @RequestBody SendChatMessageRequest request) {
        return ResponseEntity.ok(conversationService.sendMessage(user.getId(), request));
    }

    @Operation(summary = "Marquer des messages comme lus")
    @PutMapping("/messages/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markRead(@CurrentUser ApplicationUser user,
                                         @RequestBody MarkReadRequest request) {
        conversationService.markAsRead(user.getId(), request.getMessageIds());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Supprimer (soft) un de mes messages")
    @DeleteMapping("/messages/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMessage(@CurrentUser ApplicationUser user, @PathVariable Long id) {
        conversationService.deleteMessage(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Uploader une pièce jointe (image) → renvoie l'URL")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeFile(file);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
