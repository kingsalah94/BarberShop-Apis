package com.salahtech.BarberShop_Apis.repositories;


import com.salahtech.BarberShop_Apis.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Pour récupérer les notifs d'un utilisateur, triées par date (les plus récentes en premier)
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}