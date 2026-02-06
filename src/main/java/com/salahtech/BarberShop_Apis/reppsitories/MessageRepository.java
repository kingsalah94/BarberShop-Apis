package com.salahtech.BarberShop_Apis.reppsitories;

import com.salahtech.BarberShop_Apis.models.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Récupérer les messages entre deux utilisateurs (Conversation)
    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender.id = :user1 AND m.receiver.id = :user2) OR " +
            "(m.sender.id = :user2 AND m.receiver.id = :user1) " +
            "ORDER BY m.sentAt ASC")
    List<Message> findConversation(@Param("user1") Long userId1, @Param("user2") Long userId2);

    // Récupérer les derniers messages reçus par l'utilisateur (Inbox)
    // Cette requête complexe permet d'avoir le dernier message de chaque
    // conversation
    @Query("SELECT m FROM Message m WHERE m.id IN (" +
            "   SELECT MAX(m2.id) FROM Message m2 " +
            "   WHERE m2.sender.id = :userId OR m2.receiver.id = :userId " +
            "   GROUP BY CASE WHEN m2.sender.id = :userId THEN m2.receiver.id ELSE m2.sender.id END" +
            ") ORDER BY m.sentAt DESC")
    List<Message> findRecentConversations(@Param("userId") Long userId);

    // Compter les messages non lus
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.isRead = false")
    Long countUnreadMessages(@Param("userId") Long userId);

    // Marquer comme lu
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.sender.id = :senderId AND m.receiver.id = :receiverId")
    void markConversationAsRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
}