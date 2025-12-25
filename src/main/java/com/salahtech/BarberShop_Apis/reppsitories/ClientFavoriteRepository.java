package com.salahtech.BarberShop_Apis.reppsitories;

import com.salahtech.BarberShop_Apis.models.ClientFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientFavoriteRepository extends JpaRepository<ClientFavorite, Long> {

    /**
     * Find all favorites for a specific client
     */
    @Query("SELECT cf FROM ClientFavorite cf JOIN FETCH cf.barber b JOIN FETCH b.user WHERE cf.client.id = :clientId ORDER BY cf.addedAt DESC")
    List<ClientFavorite> findByClientId(@Param("clientId") Long clientId);

    /**
     * Find favorite by client and barber
     */
    Optional<ClientFavorite> findByClientIdAndBarberId(Long clientId, Long barberId);

    /**
     * Check if a barber is favorited by a client
     */
    boolean existsByClientIdAndBarberId(Long clientId, Long barberId);

    /**
     * Count favorites for a client
     */
    Long countByClientId(Long clientId);

    /**
     * Count how many clients favorited a barber
     */
    Long countByBarberId(Long barberId);

    /**
     * Delete favorite by client and barber
     */
    void deleteByClientIdAndBarberId(Long clientId, Long barberId);

    /**
     * Get most visited barbers for a client
     */
    @Query("SELECT cf FROM ClientFavorite cf WHERE cf.client.id = :clientId ORDER BY cf.visitCount DESC, cf.lastVisit DESC")
    List<ClientFavorite> findTopFavoritesByClientId(@Param("clientId") Long clientId);
}
