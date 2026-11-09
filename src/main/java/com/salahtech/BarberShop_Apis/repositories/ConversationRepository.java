package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.Enums.ConversationType;
import com.salahtech.BarberShop_Apis.models.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /** Conversations d'un utilisateur (en tant que participant1 OU participant2). */
    @Query("SELECT c FROM Conversation c WHERE c.participant1.id = :uid OR c.participant2.id = :uid "
            + "ORDER BY c.lastMessageAt DESC")
    Page<Conversation> findForUser(@Param("uid") Long userId, Pageable pageable);

    /** Conversation existante entre deux utilisateurs pour un type donné (dédoublonnage). */
    @Query("SELECT c FROM Conversation c WHERE c.type = :type AND ("
            + "(c.participant1.id = :a AND c.participant2.id = :b) OR "
            + "(c.participant1.id = :b AND c.participant2.id = :a))")
    Optional<Conversation> findBetween(@Param("a") Long a, @Param("b") Long b,
                                       @Param("type") ConversationType type);
}
