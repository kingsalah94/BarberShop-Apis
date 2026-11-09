package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.ConversationMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Diffusion temps réel des messages via STOMP. On pousse chaque nouveau message
 * sur le topic « inbox » du destinataire : {@code /topic/user/{receiverId}/inbox}.
 * Un seul topic par utilisateur ⇒ pas de doublon ; le front décide d'ajouter au
 * fil actif ou d'incrémenter le badge (cf. addIncomingMessage).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessagingNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyNewMessage(Long receiverId, ConversationMessageDto msg) {
        if (receiverId == null) return;
        String dest = "/topic/user/" + receiverId + "/inbox";
        messagingTemplate.convertAndSend(dest, toEvent(msg));
        log.debug("WS message poussé vers {} (conv={}, msg={})", dest, msg.getConversationId(), msg.getId());
    }

    /** Payload plat attendu par le front : champs du message + `type` = MESSAGE. */
    private Map<String, Object> toEvent(ConversationMessageDto m) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("type", "MESSAGE");
        e.put("id", m.getId());
        e.put("conversationId", m.getConversationId());
        e.put("senderId", m.getSenderId());
        e.put("senderName", m.getSenderName());
        e.put("senderAvatar", m.getSenderAvatar());
        e.put("content", m.getContent());
        e.put("messageType", m.getMessageType());
        e.put("replyToId", m.getReplyToId());
        e.put("createdAt", m.getCreatedAt());
        e.put("readAt", m.getReadAt());
        e.put("editedAt", m.getEditedAt());
        e.put("isRead", m.getIsRead());
        e.put("isDeleted", m.getIsDeleted());
        return e;
    }
}
