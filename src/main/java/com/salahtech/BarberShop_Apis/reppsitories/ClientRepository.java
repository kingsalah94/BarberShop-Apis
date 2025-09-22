package com.salahtech.BarberShop_Apis.reppsitories;

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

    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.bookings WHERE c.id = :id")
    Optional<Client> findByIdWithBookings(@Param("id") Long id);

    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.bookings WHERE c.id = :id")
    Optional<Client> findByIdWithFullDetails(@Param("id") Long id);

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

    @Query("SELECT c FROM Client c WHERE SIZE(c.bookings) > 0")
    List<Client> findClientsWithBookings();

    @Query("SELECT c FROM Client c WHERE SIZE(c.bookings) = 0")
    List<Client> findClientsWithoutBookings();

    @Query("SELECT c FROM Client c WHERE SIZE(c.bookings) > 0")
    Page<Client> findClientsWithBookings(Pageable pageable);

    @Query("SELECT c FROM Client c WHERE SIZE(c.bookings) = 0")
    Page<Client> findClientsWithoutBookings(Pageable pageable);

    // ========== Top clients et statistiques ==========

    @Query("""
            SELECT c FROM Client c
            LEFT JOIN c.bookings b
            WHERE b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED
            GROUP BY c.id, c.firstName, c.lastName, c.email, c.phone, c.createdAt, c.updatedAt
            ORDER BY COUNT(b) DESC
            """)
    List<Client> findTopClientsByCompletedBookings(Pageable pageable);

    @Query("""
            SELECT c, COUNT(b) as bookingCount
            FROM Client c
            LEFT JOIN c.bookings b
            WHERE b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED
            GROUP BY c
            ORDER BY bookingCount DESC
            """)
    List<Object[]> findTopClientsWithBookingCount(Pageable pageable);

    @Query("""
            SELECT c, SUM(b.totalPrice) as totalSpent
            FROM Client c
            LEFT JOIN c.bookings b
            WHERE b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED
            GROUP BY c
            ORDER BY totalSpent DESC
            """)
    List<Object[]> findTopClientsByRevenue(Pageable pageable);

    // ========== Clients inactifs ==========

    @Query("""
            SELECT c FROM Client c
            WHERE NOT EXISTS (
                SELECT b FROM Booking b
                WHERE b.client = c
                AND b.startAt > :date
            )
            """)
    List<Client> findInactiveClientsSince(@Param("date") LocalDateTime date);

    @Query("""
            SELECT c FROM Client c
            WHERE c.id NOT IN (
                SELECT DISTINCT b.client.id FROM Booking b
                WHERE b.startAt > :date
                OR b.bookingDate > :date
            )
            """)
    Page<Client> findClientsWithoutRecentBookings(@Param("date") LocalDateTime date, Pageable pageable);

    // ========== Requêtes par barbier ==========

    @Query("""
            SELECT DISTINCT c FROM Client c
            JOIN c.bookings b
            WHERE b.barber.id = :barberId
            """)
    List<Client> findClientsByBarberId(@Param("barberId") Long barberId);

    @Query("""
            SELECT DISTINCT c FROM Client c
            JOIN c.bookings b
            WHERE b.barber.id = :barberId
            AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED
            """)
    List<Client> findRegularClientsByBarberId(@Param("barberId") Long barberId);

    // ========== Statistiques globales ==========

    @Query("SELECT COUNT(c) FROM Client c WHERE c.createdAt >= :date")
    Long countNewClientsSince(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(DISTINCT c) FROM Client c JOIN c.bookings b WHERE b.createdAt >= :date")
    Long countActiveClientsSince(@Param("date") LocalDateTime date);

    @Query("""
            SELECT
                COUNT(c) as total,
                COUNT(CASE WHEN SIZE(c.bookings) > 0 THEN 1 END) as withBookings,
                COUNT(CASE WHEN SIZE(c.bookings) = 0 THEN 1 END) as withoutBookings
            FROM Client c
            """)
    List<Object[]> getClientStatistics();

    // ========== Recherches avancées avec préférences ==========

    @Query("SELECT c FROM Client c WHERE c.preferences LIKE %:preference%")
    List<Client> findByPreferencesContaining(@Param("preference") String preference);

    @Query("SELECT c FROM Client c WHERE c.notes IS NOT NULL AND c.notes != ''")
    List<Client> findClientsWithNotes();

    // ========== Requêtes natives pour performances ==========

    @Query(value = """
            SELECT c.*, COUNT(b.id) as booking_count
            FROM clients c
            LEFT JOIN bookings b ON c.id = b.client_id
            WHERE b.status = 'COMPLETED'
            GROUP BY c.id
            HAVING COUNT(b.id) >= :minBookings
            ORDER BY booking_count DESC
            """, nativeQuery = true)
    List<Client> findLoyalClients(@Param("minBookings") int minBookings);

    @Query(value = """
            SELECT c.* FROM clients c
            WHERE c.id IN (
                SELECT b.client_id
                FROM bookings b
                WHERE b.barber_service_id = :serviceId
                GROUP BY b.client_id
                HAVING COUNT(*) >= :minCount
            )
            """, nativeQuery = true)
    List<Client> findFrequentServiceUsers(@Param("serviceId") Long serviceId, @Param("minCount") int minCount);

    // ========== Suppression ==========

    void deleteByUser_Id(Long userId);

    @Query("DELETE FROM Client c WHERE c.createdAt < :date AND SIZE(c.bookings) = 0")
    void deleteInactiveClientsOlderThan(@Param("date") LocalDateTime date);
}