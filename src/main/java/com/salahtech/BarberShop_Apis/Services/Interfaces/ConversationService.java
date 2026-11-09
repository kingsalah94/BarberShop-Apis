package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.ContactDto;
import com.salahtech.BarberShop_Apis.Dtos.ConversationDto;
import com.salahtech.BarberShop_Apis.Dtos.ConversationListResponse;
import com.salahtech.BarberShop_Apis.Dtos.ConversationMessageDto;
import com.salahtech.BarberShop_Apis.Dtos.CreateConversationRequest;
import com.salahtech.BarberShop_Apis.Dtos.MessagePageResponse;
import com.salahtech.BarberShop_Apis.Dtos.SendChatMessageRequest;

import java.util.List;

public interface ConversationService {

    /** Utilisateurs contactables pour démarrer une conversation. */
    List<ContactDto> searchContacts(Long currentUserId, String q, int limit);

    ConversationListResponse listConversations(Long userId, int page, int size);

    ConversationDto createConversation(Long userId, CreateConversationRequest request);

    void archive(Long userId, Long conversationId);

    MessagePageResponse listMessages(Long userId, Long conversationId, int page, int size);

    ConversationMessageDto sendMessage(Long userId, SendChatMessageRequest request);

    void markAsRead(Long userId, List<Long> messageIds);

    void deleteMessage(Long userId, Long messageId);

    long getTotalUnread(Long userId);
}
