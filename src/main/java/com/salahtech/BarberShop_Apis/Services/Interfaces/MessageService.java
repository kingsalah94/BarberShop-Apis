package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.MessageDto;
import java.util.List;

public interface MessageService {
    MessageDto sendMessage(Long senderId, Long receiverId, String content, Long bookingId);

    List<MessageDto> getConversation(Long currentUserId, Long otherUserId);

    List<MessageDto> getMyConversations(Long currentUserId);

    Long getUnreadCount(Long userId);

    void markAsRead(Long currentUserId, Long senderId);
}