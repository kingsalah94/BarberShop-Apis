package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.Enums.QueueStatus;
import com.salahtech.BarberShop_Apis.models.QueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    @Query("SELECT q FROM QueueEntry q WHERE q.salon.id = :salonId AND q.status IN :statuses ORDER BY q.checkInTime ASC")
    List<QueueEntry> findActiveBySalon(@Param("salonId") Long salonId,
                                       @Param("statuses") List<QueueStatus> statuses);

    @Query("SELECT q FROM QueueEntry q WHERE q.salon.id = :salonId AND q.status = 'WAITING' ORDER BY q.checkInTime ASC")
    List<QueueEntry> findWaitingBySalon(@Param("salonId") Long salonId);

    long countBySalon_IdAndStatus(Long salonId, QueueStatus status);

    @Query("SELECT COUNT(q) FROM QueueEntry q WHERE q.salon.id = :salonId AND q.status = 'WAITING' AND q.checkInTime < :checkInTime")
    long countAheadOf(@Param("salonId") Long salonId, @Param("checkInTime") LocalDateTime checkInTime);

    @Query("SELECT COUNT(q) FROM QueueEntry q WHERE q.salon.id = :salonId AND q.checkInTime >= :startOfDay")
    long countByDayBySalon(@Param("salonId") Long salonId, @Param("startOfDay") LocalDateTime startOfDay);

    /** Prestations terminées d'un salon sur une journée (pour caisse / rattrapage / reçus). */
    @Query("SELECT q FROM QueueEntry q WHERE q.salon.id = :salonId AND q.status = 'COMPLETED' "
            + "AND q.endedAt >= :startOfDay AND q.endedAt < :endOfDay ORDER BY q.endedAt DESC")
    List<QueueEntry> findCompletedBySalonAndDay(@Param("salonId") Long salonId,
                                                @Param("startOfDay") LocalDateTime startOfDay,
                                                @Param("endOfDay") LocalDateTime endOfDay);

    /** Entrée active déjà créée pour cette réservation salon (idempotence confirm/arrivée). */
    Optional<QueueEntry> findFirstBySalonBookingIdAndStatusInOrderByCheckInTimeDesc(
            Long salonBookingId, List<QueueStatus> statuses);

    /** Entrées actives d'un client, tous salons confondus (« mes tickets »). */
    @Query("SELECT q FROM QueueEntry q WHERE q.client.id = :clientId "
            + "AND q.status IN :statuses ORDER BY q.checkInTime DESC")
    List<QueueEntry> findActiveByClient(@Param("clientId") Long clientId,
                                        @Param("statuses") List<QueueStatus> statuses);

    /** Entrée active du client dans un salon (pour « ma place dans la file »). */
    @Query("SELECT q FROM QueueEntry q WHERE q.salon.id = :salonId AND q.client.id = :clientId "
            + "AND q.status IN :statuses ORDER BY q.checkInTime DESC")
    List<QueueEntry> findActiveByClientAndSalon(@Param("salonId") Long salonId,
                                                @Param("clientId") Long clientId,
                                                @Param("statuses") List<QueueStatus> statuses);

    /** Salon de l'entrée (contrôle d'accès multi-tenant, sans charger l'entité). */
    @Query("SELECT q.salon.id FROM QueueEntry q WHERE q.id = :id")
    Optional<Long> findSalonIdById(@Param("id") Long id);

    /** Client propriétaire de l'entrée (null si walk-in anonyme). */
    @Query("SELECT c.id FROM QueueEntry q LEFT JOIN q.client c WHERE q.id = :id")
    Optional<Long> findClientIdById(@Param("id") Long id);

    // ----- Fidélité : passages sans RDV d'un client identifié -----
    long countBySalon_IdAndClient_IdAndStatusAndSalonBookingIdIsNull(Long salonId, Long clientId, QueueStatus status);

    List<QueueEntry> findByClient_IdAndStatusAndSalonBookingIdIsNull(Long clientId, QueueStatus status);
}
