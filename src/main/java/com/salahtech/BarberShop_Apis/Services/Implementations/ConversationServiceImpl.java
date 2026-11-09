package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.ContactDto;
import com.salahtech.BarberShop_Apis.Dtos.ConversationDto;
import com.salahtech.BarberShop_Apis.Dtos.ConversationListResponse;
import com.salahtech.BarberShop_Apis.Dtos.ConversationMessageDto;
import com.salahtech.BarberShop_Apis.Dtos.CreateConversationRequest;
import com.salahtech.BarberShop_Apis.Dtos.MessagePageResponse;
import com.salahtech.BarberShop_Apis.Dtos.SendChatMessageRequest;
import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.Enums.ConversationStatus;
import com.salahtech.BarberShop_Apis.Enums.ConversationType;
import com.salahtech.BarberShop_Apis.Enums.MessageType;
import com.salahtech.BarberShop_Apis.Exceptions.BusinessException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ConversationService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.models.Conversation;
import com.salahtech.BarberShop_Apis.models.Message;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.repositories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.repositories.BookingRepository;
import com.salahtech.BarberShop_Apis.repositories.ConversationRepository;
import com.salahtech.BarberShop_Apis.repositories.MessageRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ApplicationUserRepository userRepository;
    private final SalonRepository salonRepository;
    private final BookingRepository bookingRepository;
    private final MessagingNotifier notifier;

    @Override
    @Transactional(readOnly = true)
    public ConversationListResponse listConversations(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size <= 0 ? 20 : size);
        Page<Conversation> result = conversationRepository.findForUser(userId, pageable);
        List<ConversationDto> dtos = result.getContent().stream()
                .map(c -> toDto(c, userId))
                .toList();
        return ConversationListResponse.builder()
                .conversations(dtos)
                .totalUnread(messageRepository.countByReceiver_IdAndIsReadFalse(userId))
                .page(result.getNumber())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    public ConversationDto createConversation(Long userId, CreateConversationRequest req) {
        if (req.getParticipantId() == null || req.getParticipantId().equals(userId)) {
            throw new BusinessException("Participant invalide");
        }
        ApplicationUser me = getUser(userId);
        ApplicationUser other = getUser(req.getParticipantId());
        ConversationType type = req.getType() != null ? req.getType() : ConversationType.CLIENT_BARBER;

        Conversation conv = conversationRepository.findBetween(userId, other.getId(), type)
                .orElseGet(() -> {
                    Conversation c = Conversation.builder()
                            .type(type)
                            .participant1(me)
                            .participant2(other)
                            .status(ConversationStatus.ACTIVE)
                            .salon(resolveSalon(req.getSalonId()))
                            .build();
                    return conversationRepository.save(c);
                });

        if (req.getInitialMessage() != null && !req.getInitialMessage().isBlank()) {
            persistMessage(conv, me, other, req.getInitialMessage(), MessageType.TEXT, null, null);
        }
        return toDto(conv, userId);
    }

    @Override
    public void archive(Long userId, Long conversationId) {
        Conversation conv = getConversationForUser(conversationId, userId);
        conv.setStatus(ConversationStatus.ARCHIVED);
        conversationRepository.save(conv);
    }

    @Override
    public MessagePageResponse listMessages(Long userId, Long conversationId, int page, int size) {
        Conversation conv = getConversationForUser(conversationId, userId);
        // Marquer comme lus les messages reçus dans cette conversation.
        messageRepository.markConversationReadForReceiver(conv.getId(), userId);

        Pageable pageable = PageRequest.of(Math.max(page, 0), size <= 0 ? 50 : size);
        Page<Message> result = messageRepository.findByConversation_IdOrderBySentAtDesc(conv.getId(), pageable);
        List<ConversationMessageDto> dtos = result.getContent().stream().map(this::toMessageDto).toList();
        return MessagePageResponse.builder()
                .messages(dtos)
                .page(result.getNumber())
                .totalPages(result.getTotalPages())
                .hasMore(result.hasNext())
                .build();
    }

    @Override
    public ConversationMessageDto sendMessage(Long userId, SendChatMessageRequest req) {
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new BusinessException("Le contenu du message est requis");
        }
        Conversation conv = getConversationForUser(req.getConversationId(), userId);
        ApplicationUser me = getUser(userId);
        ApplicationUser receiver = otherParticipant(conv, userId);

        Message reply = null;
        if (req.getReplyToId() != null) {
            reply = messageRepository.findById(req.getReplyToId()).orElse(null);
        }
        Booking booking = null;
        if (req.getBookingId() != null) {
            booking = bookingRepository.findById(req.getBookingId()).orElse(null);
        }

        Message saved = persistMessage(conv, me, receiver, req.getContent(),
                req.getMessageType() != null ? req.getMessageType() : MessageType.TEXT, reply, booking);

        ConversationMessageDto dto = toMessageDto(saved);
        notifier.notifyNewMessage(receiver.getId(), dto);
        return dto;
    }

    @Override
    public void markAsRead(Long userId, List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return;
        messageRepository.markReadByIds(messageIds, userId);
    }

    @Override
    public void deleteMessage(Long userId, Long messageId) {
        Message m = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message introuvable: " + messageId));
        if (m.getSender() == null || !m.getSender().getId().equals(userId)) {
            throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres messages");
        }
        m.setIsDeleted(true);
        m.setContent("Message supprimé");
        m.setEditedAt(LocalDateTime.now());
        messageRepository.save(m);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalUnread(Long userId) {
        return messageRepository.countByReceiver_IdAndIsReadFalse(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactDto> searchContacts(Long currentUserId, String q, int limit) {
        ApplicationUser me = getUser(currentUserId);
        // Un client ne contacte que le staff ; le staff (barbier/owner/admin) contacte tout le monde.
        ApplicationUserType exclude =
                me.getUserType() == ApplicationUserType.CLIENT ? ApplicationUserType.CLIENT : null;
        int capped = (limit <= 0 || limit > 50) ? 20 : limit;
        return userRepository
                .searchContacts(currentUserId, q == null ? "" : q.trim(), exclude,
                        PageRequest.of(0, capped))
                .stream()
                .map(u -> ContactDto.builder()
                        .id(u.getId())
                        .name(fullName(u))
                        .email(u.getEmail())
                        .userType(u.getUserType())
                        .avatar(u.getProfileImageUrl())
                        .build())
                .toList();
    }

    // ===== Helpers =====

    private Message persistMessage(Conversation conv, ApplicationUser sender, ApplicationUser receiver,
                                   String content, MessageType type, Message reply, Booking booking) {
        Message m = Message.builder()
                .conversation(conv)
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .messageType(type != null ? type : MessageType.TEXT)
                .replyTo(reply)
                .booking(booking)
                .isRead(false)
                .isDeleted(false)
                .build();
        Message saved = messageRepository.save(m);
        conv.setLastMessageContent(previewOf(saved));
        conv.setLastMessageAt(saved.getSentAt());
        conversationRepository.save(conv);
        return saved;
    }

    private String previewOf(Message m) {
        if (m.getMessageType() == MessageType.IMAGE) return "📷 Photo";
        if (m.getMessageType() == MessageType.DOCUMENT) return "📎 Document";
        return m.getContent();
    }

    private Conversation getConversationForUser(Long conversationId, Long userId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable: " + conversationId));
        if (!isParticipant(conv, userId)) {
            throw new AccessDeniedException("Accès refusé à cette conversation");
        }
        return conv;
    }

    private boolean isParticipant(Conversation c, Long userId) {
        return (c.getParticipant1() != null && c.getParticipant1().getId().equals(userId))
                || (c.getParticipant2() != null && c.getParticipant2().getId().equals(userId));
    }

    private ApplicationUser otherParticipant(Conversation c, Long userId) {
        return c.getParticipant1() != null && c.getParticipant1().getId().equals(userId)
                ? c.getParticipant2() : c.getParticipant1();
    }

    private ApplicationUser getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable: " + id));
    }

    private Salon resolveSalon(Long salonId) {
        if (salonId == null) return null;
        return salonRepository.findById(salonId).orElse(null);
    }

    private ConversationDto toDto(Conversation c, Long currentUserId) {
        ApplicationUser p1 = c.getParticipant1();
        ApplicationUser p2 = c.getParticipant2();
        return ConversationDto.builder()
                .id(c.getId())
                .type(c.getType())
                .participant1Id(p1 != null ? p1.getId() : null)
                .participant1Name(fullName(p1))
                .participant1Avatar(p1 != null ? p1.getProfileImageUrl() : null)
                .participant2Id(p2 != null ? p2.getId() : null)
                .participant2Name(fullName(p2))
                .participant2Avatar(p2 != null ? p2.getProfileImageUrl() : null)
                .client(clientParticipantId(c))
                .salonId(c.getSalon() != null ? c.getSalon().getId() : null)
                .status(c.getStatus())
                .lastMessageContent(c.getLastMessageContent())
                .lastMessageAt(c.getLastMessageAt())
                .unreadCount((int) messageRepository
                        .countByConversation_IdAndReceiver_IdAndIsReadFalse(c.getId(), currentUserId))
                .createdAt(c.getCreatedAt())
                .build();
    }

    private ConversationMessageDto toMessageDto(Message m) {
        ApplicationUser s = m.getSender();
        return ConversationMessageDto.builder()
                .id(m.getId())
                .conversationId(m.getConversation() != null ? m.getConversation().getId() : null)
                .senderId(s != null ? s.getId() : null)
                .senderName(fullName(s))
                .senderAvatar(s != null ? s.getProfileImageUrl() : null)
                .content(m.getContent())
                .messageType(m.getMessageType())
                .replyToId(m.getReplyTo() != null ? m.getReplyTo().getId() : null)
                .bookingId(m.getBooking() != null ? m.getBooking().getId() : null)
                .createdAt(m.getSentAt())
                .readAt(m.getReadAt())
                .editedAt(m.getEditedAt())
                .isRead(Boolean.TRUE.equals(m.getIsRead()))
                .isDeleted(Boolean.TRUE.equals(m.getIsDeleted()))
                .build();
    }

    /** Id du participant de type CLIENT (sinon participant1 par défaut). */
    private Long clientParticipantId(Conversation c) {
        if (c.getParticipant1() != null && c.getParticipant1().getUserType() == ApplicationUserType.CLIENT)
            return c.getParticipant1().getId();
        if (c.getParticipant2() != null && c.getParticipant2().getUserType() == ApplicationUserType.CLIENT)
            return c.getParticipant2().getId();
        return c.getParticipant1() != null ? c.getParticipant1().getId() : null;
    }

    private String fullName(ApplicationUser u) {
        if (u == null) return null;
        String n = ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                + (u.getLastName() != null ? u.getLastName() : "")).trim();
        return n.isEmpty() ? u.getEmail() : n;
    }
}
