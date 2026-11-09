package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.Dtos.BarberClientSummaryDto;
import com.salahtech.BarberShop_Apis.models.Client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {

    // ========== Recherches de base ==========

    Optional<Client> findByUser_Id(Long userId);

    Optional<Client> findByEmail(String email);

    Optional<Client> findByPhone(String phone);

    // Pour éviter la confusion avec findByUser_Id
    default Optional<Client> findByUserId(Long userId) {
        return findByUser_Id(userId);
    }

    // ========== Vérifications d'existence ==========

    boolean existsByUser_Id(Long userId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    default boolean existsByUserId(Long userId) {
        return existsByUser_Id(userId);
    }

    // Vérifier l'unicité lors de la mise à jour
    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByUser_IdAndIdNot(Long userId, Long id);

    // ========== Requêtes avec fetch joins ==========

    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.user WHERE c.id = :id")
    Optional<Client> findByIdWithUser(@Param("id") Long id);

    // ========== Recherches par critères ==========

    @Query("SELECT c FROM Client c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    List<Client> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c FROM Client c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    Page<Client> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Recherche textuelle
    @Query("""
            SELECT c FROM Client c
            WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%'))
               OR c.phone LIKE CONCAT('%', :query, '%')
            """)
    Page<Client> searchByQuery(@Param("query") String query, Pageable pageable);

    // ========== Clients avec/sans réservations ==========

    // ========== Top clients et statistiques ==========

    // ========== Clients inactifs ==========

    // ========== Requêtes par barbier ==========

    // Dans ClientRepository.java
 
   
   

    // ========== Statistiques globales ==========

    @Query("SELECT COUNT(c) FROM Client c WHERE c.createdAt >= :date")
    Long countNewClientsSince(@Param("date") LocalDateTime date);

    // ========== Recherches avancées avec préférences ==========

    @Query("SELECT c FROM Client c WHERE c.preferences LIKE %:preference%")
    List<Client> findByPreferencesContaining(@Param("preference") String preference);

    @Query("SELECT c FROM Client c WHERE c.notes IS NOT NULL AND c.notes != ''")
    List<Client> findClientsWithNotes();

    // ========== Requêtes natives pour performances ==========

    // ========== Suppression ==========

    void deleteByUser_Id(Long userId);

    // Fetch clients who have had at least 3 COMPLETED bookings with a specific barber
    // @Query("SELECT DISTINCT c FROM Client c JOIN c.bookings b WHERE b.barber.id = :barberId AND b.status = 'COMPLETED' GROUP BY c HAVING COUNT(b) >= 3")
    // List<Client> findRegularClientsByBarberId(@Param("barberId") Long barberId);

    // Count new clients registered since a specific date
    // @Query("SELECT COUNT(c) FROM Client c WHERE c.createdAt >= :date")
    // Long countNewClientsSince(@Param("date") LocalDateTime date);

    // Count active clients (those who had a booking) since a specific date
    // @Query("SELECT COUNT(DISTINCT c) FROM Client c JOIN c.bookings b WHERE b.startAt >= :date")
    // Long countActiveClientsSince(@Param("date") LocalDateTime date);
}