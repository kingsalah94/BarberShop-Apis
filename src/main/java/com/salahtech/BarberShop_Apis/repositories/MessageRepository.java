package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // ===== Messagerie à conversations =====

    /** Messages d'une conversation, du plus récent au plus ancien (paginé). */
    Page<Message> findByConversation_IdOrderBySentAtDesc(Long conversationId, Pageable pageable);

    /** Total des messages non lus reçus par l'utilisateur. */
    long countByReceiver_IdAndIsReadFalse(Long receiverId);

    /** Non-lus reçus par l'utilisateur dans une conversation donnée. */
    long countByConversation_IdAndReceiver_IdAndIsReadFalse(Long conversationId, Long receiverId);

    /** Marque comme lus les messages reçus par {@code receiverId} dans la conversation. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Message m SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP "
            + "WHERE m.conversation.id = :conversationId AND m.receiver.id = :receiverId AND m.isRead = false")
    int markConversationReadForReceiver(@Param("conversationId") Long conversationId,
                                        @Param("receiverId") Long receiverId);

    /** Marque comme lus des messages précis, à condition d'en être le destinataire. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Message m SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP "
            + "WHERE m.id IN :ids AND m.receiver.id = :receiverId AND m.isRead = false")
    int markReadByIds(@Param("ids") List<Long> ids, @Param("receiverId") Long receiverId);
}