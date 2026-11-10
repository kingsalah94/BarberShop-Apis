package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.LoyaltyReward;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyRewardRepository extends JpaRepository<LoyaltyReward, Long> {

    long countByClient_IdAndSalon_Id(Long clientId, Long salonId);

    boolean existsByQueueEntry_Id(Long queueEntryId);
}
