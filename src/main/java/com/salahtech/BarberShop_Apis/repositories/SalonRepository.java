package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.Salon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalonRepository extends JpaRepository<Salon, Long> {

    List<Salon> findByOwner_Id(Long ownerId);

    /**
     * Verrou pessimiste (SELECT ... FOR UPDATE) sur la ligne du salon, tenu jusqu'au
     * commit : sérialise les opérations concurrentes d'un même salon (réservation,
     * numéro de ticket, appel du suivant). À appeler dans une transaction.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT s.id FROM Salon s WHERE s.id = :id")
    java.util.Optional<Long> lockById(@org.springframework.data.repository.query.Param("id") Long id);
}