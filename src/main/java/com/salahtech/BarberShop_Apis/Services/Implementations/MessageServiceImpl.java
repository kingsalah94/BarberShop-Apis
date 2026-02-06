package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.MessageDto;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.MessageService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.models.Message;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;
import com.salahtech.BarberShop_Apis.reppsitories.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ApplicationUserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Override
    public MessageDto sendMessage(Long senderId, Long receiverId, String content, Long bookingId) {
        ApplicationUser sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Expéditeur introuvable"));
        ApplicationUser receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Destinataire introuvable"));

        Booking booking = null;
        if (bookingId != null) {
            booking = bookingRepository.findById(bookingId).orElse(null);
        }

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .booking(booking)
                .isRead(false)
                .build();

        Message saved = messageRepository.save(message);
        return mapToDto(saved, senderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageDto> getConversation(Long currentUserId, Long otherUserId) {
        return messageRepository.findConversation(currentUserId, otherUserId)
                .stream()
                .map(m -> mapToDto(m, currentUserId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageDto> getMyConversations(Long currentUserId) {
        return messageRepository.findRecentConversations(currentUserId)
                .stream()
                .map(m -> mapToDto(m, currentUserId))
                .collect(Collectors.toList());
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return messageRepository.countUnreadMessages(userId);
    }

    @Override
    public void markAsRead(Long currentUserId, Long senderId) {
        messageRepository.markConversationAsRead(senderId, currentUserId);
    }

    private MessageDto mapToDto(Message m, Long currentUserId) {
        return MessageDto.builder()
                .id(m.getId())
                .senderId(m.getSender().getId())
                .senderName(m.getSender().getFirstName() + " " + m.getSender().getLastName())
                .senderAvatar(m.getSender().getProfileImageUrl())
                .receiverId(m.getReceiver().getId())
                .receiverName(m.getReceiver().getFirstName() + " " + m.getReceiver().getLastName())
                .content(m.getContent())
                .isRead(m.getIsRead())
                .sentAt(m.getSentAt())
                .bookingId(m.getBooking() != null ? m.getBooking().getId() : null)
                .isMe(m.getSender().getId().equals(currentUserId))
                .build();
    }
}