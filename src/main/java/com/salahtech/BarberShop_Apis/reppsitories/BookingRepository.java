package com.salahtech.BarberShop_Apis.reppsitories;

import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.models.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

  // ✅ TA MÉTHODE ORIGINALE (Lock Pessimiste) - CONSERVÉE
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select count(b) from Booking b
      where b.barber.id = :barberId
        and b.status in (com.salahtech.BarberShop_Apis.Enums.BookingStatus.CONFIRMED,
                         com.salahtech.BarberShop_Apis.Enums.BookingStatus.PENDING)
        and ((b.startAt < :endAt) and (b.endAt > :startAt))
      """)
  Long countConflictingBookingsWithLock(@Param("barberId") Long barberId,
      @Param("startAt") LocalDateTime startAt,
      @Param("endAt") LocalDateTime endAt);

  @Query("""
         SELECT b
         FROM Booking b
         LEFT JOIN FETCH b.client c
         LEFT JOIN FETCH c.user u
         LEFT JOIN FETCH b.barber br
         LEFT JOIN FETCH br.user
         LEFT JOIN FETCH b.barberService
         WHERE b.id = :id
      """)
  Optional<Booking> findByIdWithDetails(@Param("id") Long id);

  // Optimisation pour l'historique client
  // Pas besoin de fetcher le client/user car on les a déjà si on est sur la page
  // profil
  // Mais si tu veux être sûr : LEFT JOIN FETCH b.client c LEFT JOIN FETCH c.user
  @Query("""
         SELECT b
         FROM Booking b
         LEFT JOIN FETCH b.barber br
         LEFT JOIN FETCH br.user
         LEFT JOIN FETCH b.barberService
         WHERE b.client.id = :clientId
         ORDER BY b.startAt DESC
      """)
  List<Booking> findByClientIdWithDetails(@Param("clientId") Long clientId);

  // Elle récupère les réservations d'un barbier APRES une certaine date
  @Query("SELECT b FROM Booking b WHERE b.barber.id = :barberId AND b.startAt >= :date")
  List<Booking> findByBarberIdAndStartAtAfter(
      @Param("barberId") Long barberId,
      @Param("date") LocalDateTime date);
  // =========================================================
  // 🔧 CORRECTION POUR LE DASHBOARD (GRAPHIQUES & REVENUS)
  // J'ai ajouté COALESCE(..., 0) et corrigé les statuts ici
  // =========================================================

  // 1. REVENUS GLOBAUX (Admin) - CORRIGÉ
  @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b " +
      "WHERE b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED " +
      "AND b.startAt >= :start AND b.startAt < :end")
  BigDecimal sumGlobalRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  // Alias pour compatibilité
  default BigDecimal sumRevenueCompletedBetween(LocalDateTime from, LocalDateTime to) {
    return sumGlobalRevenueBetween(from, to);
  }

  // 2. REVENUS BARBIER (Barber) - CORRIGÉ
  @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b " +
      "WHERE b.barber.id = :barberId " +
      "AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED " +
      "AND b.startAt >= :start AND b.startAt < :end")
  BigDecimal sumRevenueByBarberBetween(@Param("barberId") Long barberId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  // 3. COMPTAGE (Courbe RDV) - ADMIN
  @Query("SELECT COUNT(b) FROM Booking b WHERE b.startAt >= :start AND b.startAt < :end")
  long countBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  // 4. COMPTAGE (Courbe RDV) - BARBIER
  @Query("SELECT COUNT(b) FROM Booking b WHERE b.barber.id = :barberId AND b.startAt >= :start AND b.startAt < :end")
  long countByBarberBetween(@Param("barberId") Long barberId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  // 5. STATUTS (Donut) - ADMIN
  @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status AND b.startAt >= :start AND b.startAt < :end")
  long countByStatusBetween(@Param("status") BookingStatus status,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  // 6. STATUTS (Donut) - BARBIER
  @Query("SELECT COUNT(b) FROM Booking b WHERE b.barber.id = :barberId AND b.status = :status AND b.startAt >= :start AND b.startAt < :end")
  long countByBarberAndStatusBetween(@Param("barberId") Long barberId,
      @Param("status") BookingStatus status,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  // =========================================================
  // ⬇️ TOUTES TES ANCIENNES MÉTHODES SONT ICI (RESTITUÉES) ⬇️
  // =========================================================

  List<Booking> findByBarberIdAndStatus(Long barberId, BookingStatus status);

  List<Booking> findByClientId(Long clientId);

  // ----- Listes / tris -----
  @Query("""
      select b from Booking b
      where b.client.user.id = :userId
      order by coalesce(b.startAt, b.bookingDate) desc
      """)
  List<Booking> findByUserIdOrderByBookingDateDesc(@Param("userId") Long userId);

  @Query("""
      select b from Booking b
      where b.barber.id = :barberId
      order by coalesce(b.startAt, b.bookingDate) asc
      """)
  List<Booking> findByBarberIdOrderByBookingDateAsc(@Param("barberId") Long barberId);

  @Query("""
      select b from Booking b
      where b.status = :status
        and coalesce(b.startAt, b.bookingDate) >= :fromDate
      order by coalesce(b.startAt, b.bookingDate) asc
      """)
  List<Booking> findByStatusAndBookingDateAfterOrderByBookingDateAsc(
      @Param("status") BookingStatus status,
      @Param("fromDate") LocalDateTime fromDate);

  // Comptage complétées
  @Query("""
      select count(b) from Booking b
      where b.barber.id = :barberId
        and b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED
      """)
  Long countCompletedBookingsByBarberId(@Param("barberId") Long barberId);

  // ----- Sélections simples -----
  List<Booking> findByClient_Id(Long clientId);

  List<Booking> findByClient_User_Id(Long userId);

  List<Booking> findByBarber_Id(Long barberId);

  List<Booking> findByBarberService_Id(Long serviceId);

  List<Booking> findByStatus(BookingStatus status);

  // ----- Périodes (sur startAt) -----
  List<Booking> findByStartAtBetween(LocalDateTime startDate, LocalDateTime endDate);

  @Query("""
      select b from Booking b
      where b.startAt >= :startDate and b.startAt < :endDate
        and b.status = :status
      """)
  List<Booking> findAllBetweenDatesAndStatus(@Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("status") BookingStatus status);

  @Query("""
      select b from Booking b
      where b.startAt >= :startDate and b.startAt < :endDate
        and b.client.user.id = :userId
      """)
  List<Booking> findAllBetweenDatesAndUserId(@Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("userId") Long userId);

  @Query("""
      select b from Booking b
      where b.startAt >= :startDate and b.startAt < :endDate
        and b.barber.id = :barberId
      """)
  List<Booking> findAllBetweenDatesAndBarberId(@Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("barberId") Long barberId);

  @Query("""
      select b from Booking b
      where b.startAt >= :startDate and b.startAt < :endDate
        and b.barberService.id = :serviceId
      """)
  List<Booking> findAllBetweenDatesAndServiceId(@Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("serviceId") Long serviceId);

  // ----- Chevauchement de créneau (conflits) -----
  @Query("""
      select b from Booking b
      where b.barber.id = :barberId
        and b.status in (com.salahtech.BarberShop_Apis.Enums.BookingStatus.CONFIRMED,
                         com.salahtech.BarberShop_Apis.Enums.BookingStatus.PENDING)
        and b.startAt < :endAt
        and b.endAt   > :startAt
      """)
  List<Booking> findConflictingBookings(@Param("barberId") Long barberId,
      @Param("startAt") LocalDateTime startAt,
      @Param("endAt") LocalDateTime endAt);

  // ----- “RDV du jour” -----
  @Query("""
      select b from Booking b
      where b.barber.id = :barberId
        and b.startAt >= :dayStart and b.startAt < :dayEnd
      order by b.startAt asc
      """)
  List<Booking> findByBarberAndDay(@Param("barberId") Long barberId,
      @Param("dayStart") LocalDateTime dayStart,
      @Param("dayEnd") LocalDateTime dayEnd);

  // Périodes
  @Query("SELECT b FROM Booking b WHERE b.bookingDate BETWEEN :startDate AND :endDate")
  List<Booking> findByBookingDateBetween(@Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  @Query("select b from Booking b where DATE(b.bookingDate) = :targetDate")
  List<Booking> findAllForDate(@Param("targetDate") LocalDate targetDate);

  // TA GROSSE REQUÊTE DE SEARCH (CONSERVÉE)
  @Query("""
        SELECT b FROM Booking b
        LEFT JOIN b.client c
        LEFT JOIN b.barber br
        LEFT JOIN b.barberService s
        WHERE
            (:status   IS NULL OR b.status = :status)
        AND (:barberId IS NULL OR br.id    = :barberId)
        AND (:clientId IS NULL OR c.id     = :clientId)
        AND COALESCE(b.startAt, b.bookingDate) >= :from
        AND COALESCE(b.startAt, b.bookingDate) <  :to
        AND (
            :q IS NULL OR :q = '' OR
            LOWER(COALESCE(c.firstName,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(COALESCE(c.lastName ,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(COALESCE(br.name    ,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(COALESCE(s.name     ,'')) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(COALESCE(b.notes    ,'')) LIKE LOWER(CONCAT('%', :q, '%'))
        )
        ORDER BY COALESCE(b.startAt, b.bookingDate) DESC
      """)
  Page<Booking> searchWithFilters(
      @Param("q") String q,
      @Param("status") BookingStatus status,
      @Param("barberId") Long barberId,
      @Param("clientId") Long clientId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      Pageable pageable);

  long count();

  long countByStatus(com.salahtech.BarberShop_Apis.Enums.BookingStatus status);

  long countByStatusAndBarberId(BookingStatus status, Long barberId);

  long countByStatusAndClientId(BookingStatus status, Long clientId);

  // Revenu Total Global (Admin)
  @Query("select coalesce(sum(b.totalPrice), 0) from Booking b where b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED")
  BigDecimal sumRevenueAllCompleted();

  // ===== MÉTHODES POUR LES STATISTIQUES AVANCÉES (Barbier) =====

  // Revenus total
  @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b " +
      "WHERE b.barber.id = :barberId AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED")
  BigDecimal calculateTotalRevenue(@Param("barberId") Long barberId);

  // Alias pour compatibilité
  default BigDecimal sumRevenueByBarber(Long barberId) {
    return calculateTotalRevenue(barberId);
  }

  // Revenus par période (Autre méthode existante)
  @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b " +
      "WHERE b.barber.id = :barberId AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED "
      +
      "AND b.startAt BETWEEN :startDate AND :endDate")
  BigDecimal calculateRevenueByPeriod(
      @Param("barberId") Long barberId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  // Nombre de clients uniques
  @Query("SELECT COUNT(DISTINCT b.client.id) FROM Booking b WHERE b.barber.id = :barberId")
  Long countUniqueClients(@Param("barberId") Long barberId);

  // Nouveaux clients ce mois
  @Query("SELECT COUNT(DISTINCT b.client.id) FROM Booking b " +
      "WHERE b.barber.id = :barberId " +
      "AND b.startAt >= :startOfMonth " +
      "AND b.client.id NOT IN (" +
      "    SELECT DISTINCT b2.client.id FROM Booking b2 " +
      "    WHERE b2.barber.id = :barberId AND b2.startAt < :startOfMonth" +
      ")")
  Long countNewClientsThisMonth(@Param("barberId") Long barberId,
      @Param("startOfMonth") LocalDateTime startOfMonth);

  // Clients récurrents
  @Query("SELECT COUNT(DISTINCT b.client.id) FROM Booking b " +
      "WHERE b.barber.id = :barberId " +
      "GROUP BY b.client.id " +
      "HAVING COUNT(b.id) > 1")
  Long countReturningClients(@Param("barberId") Long barberId);

  // Note moyenne
  @Query("SELECT AVG(b.rating) FROM Booking b " +
      "WHERE b.barber.id = :barberId AND b.rating IS NOT NULL")
  Double calculateAverageRating(@Param("barberId") Long barberId);

  // Nombre total d'avis
  @Query("SELECT COUNT(b) FROM Booking b " +
      "WHERE b.barber.id = :barberId AND b.rating IS NOT NULL")
  Long countReviews(@Param("barberId") Long barberId);

  // Prochains rendez-vous
  @Query("SELECT b FROM Booking b " +
      "WHERE b.barber.id = :barberId " +
      "AND b.startAt >= :now " +
      "AND b.status IN (com.salahtech.BarberShop_Apis.Enums.BookingStatus.CONFIRMED, com.salahtech.BarberShop_Apis.Enums.BookingStatus.PENDING) "
      +
      "ORDER BY b.startAt ASC")
  List<Booking> findUpcomingBookings(
      @Param("barberId") Long barberId,
      @Param("now") LocalDateTime now);

  // Services populaires
  @Query("SELECT b.barberService.id as serviceId, b.barberService.name as serviceName, " +
      "COUNT(b) as bookingCount, SUM(b.totalPrice) as totalRevenue, " +
      "AVG(b.rating) as averageRating " +
      "FROM Booking b " +
      "WHERE b.barber.id = :barberId " +
      "GROUP BY b.barberService.id, b.barberService.name " +
      "ORDER BY COUNT(b) DESC")
  List<Object[]> findPopularServices(@Param("barberId") Long barberId);

  // Tendance hebdomadaire
  @Query("SELECT DATE(b.startAt) as bookingDate, " +
      "COUNT(b) as bookingCount, " +
      "COALESCE(SUM(CASE WHEN b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED THEN b.totalPrice ELSE 0 END), 0) as revenue "
      +
      "FROM Booking b " +
      "WHERE b.barber.id = :barberId " +
      "AND b.startAt BETWEEN :startDate AND :endDate " +
      "GROUP BY DATE(b.startAt) " +
      "ORDER BY DATE(b.startAt)")
  List<Object[]> findBookingTrend(
      @Param("barberId") Long barberId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  // Réservations par période
  @Query("SELECT COUNT(b) FROM Booking b " +
      "WHERE b.barber.id = :barberId " +
      "AND b.startAt BETWEEN :startDate AND :endDate " +
      "AND b.status != com.salahtech.BarberShop_Apis.Enums.BookingStatus.CANCELLED")
  Long countBookingsByPeriod(
      @Param("barberId") Long barberId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  // ===== MÉTHODES CLIENT =====

  List<Booking> findByClientIdAndStatus(Long clientId, BookingStatus status);

  @Query("SELECT b FROM Booking b WHERE b.startAt BETWEEN :startDate AND :endDate")
  List<Booking> findByDateRange(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  @Query("SELECT b FROM Booking b WHERE b.barber.id = :barberId " +
      "AND b.startAt BETWEEN :startDate AND :endDate")
  List<Booking> findByBarberIdAndDateRange(
      @Param("barberId") Long barberId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  Long countByClientId(Long clientId);

  Long countByClientIdAndStatus(Long clientId, BookingStatus status);

  Long countByClientIdAndStatusIn(Long clientId, List<BookingStatus> statuses);

  long countByBarberId(Long barberId);

  long countByBarberIdAndStatus(Long barberId, BookingStatus status);

  // ===== FINANCIAL QUERIES (Client) =====

  @Query("SELECT SUM(b.totalPrice) FROM Booking b " +
      "WHERE b.client.id = :clientId AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED")
  Double getTotalSpentByClient(@Param("clientId") Long clientId);

  @Query("SELECT SUM(b.totalPrice) FROM Booking b " +
      "WHERE b.barber.id = :barberId AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED")
  Double getTotalRevenueByBarber(@Param("barberId") Long barberId);

  @Query("SELECT SUM(b.totalPrice) FROM Booking b " +
      "WHERE b.barber.id = :barberId AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED "
      +
      "AND b.startAt BETWEEN :startDate AND :endDate")
  Double getRevenueByBarberAndDateRange(
      @Param("barberId") Long barberId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  // ===== BOOKING HISTORY =====

  Page<Booking> findByClientIdOrderByStartAtDesc(Long clientId, Pageable pageable);

  @Query("SELECT b FROM Booking b WHERE b.client.id = :clientId " +
      "AND b.status IN (com.salahtech.BarberShop_Apis.Enums.BookingStatus.CONFIRMED, com.salahtech.BarberShop_Apis.Enums.BookingStatus.PENDING) "
      +
      "AND b.startAt >= :now " +
      "ORDER BY b.startAt ASC")
  List<Booking> findUpcomingBookingsByClientId(
      @Param("clientId") Long clientId,
      @Param("now") LocalDateTime now);

  @Query("SELECT b FROM Booking b WHERE b.client.id = :clientId " +
      "AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED " +
      "ORDER BY b.startAt DESC")
  Page<Booking> findPastBookingsByClientId(Long clientId, Pageable pageable);

  @Query("SELECT b FROM Booking b WHERE b.client.id = :clientId " +
      "AND b.barber.id = :barberId " +
      "ORDER BY b.startAt DESC")
  List<Booking> findByClientIdAndBarberId(
      @Param("clientId") Long clientId,
      @Param("barberId") Long barberId);

  @Query("SELECT COUNT(b) FROM Booking b WHERE b.client.id = :clientId " +
      "AND b.barber.id = :barberId AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED")
  Long countVisitsByClientAndBarber(
      @Param("clientId") Long clientId,
      @Param("barberId") Long barberId);

  Page<Booking> findByBarberIdOrderByStartAtDesc(Long barberId, Pageable pageable);

  @Query("SELECT b FROM Booking b WHERE b.barber.id = :barberId " +
      "AND b.status IN (com.salahtech.BarberShop_Apis.Enums.BookingStatus.CONFIRMED, com.salahtech.BarberShop_Apis.Enums.BookingStatus.PENDING) "
      +
      "AND b.startAt >= :now " +
      "ORDER BY b.startAt ASC")
  List<Booking> findUpcomingBookingsByBarberId(
      @Param("barberId") Long barberId,
      @Param("now") LocalDateTime now);

  @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.barber.id = :barberId " +
      "AND b.status NOT IN (com.salahtech.BarberShop_Apis.Enums.BookingStatus.CANCELLED) " +
      "AND ((b.startAt < :endAt AND b.endAt > :startAt))")
  boolean isTimeSlotTaken(
      @Param("barberId") Long barberId,
      @Param("startAt") LocalDateTime startAt,
      @Param("endAt") LocalDateTime endAt);

  // ===== MOST VISITED =====

  @Query("SELECT b.barber.id, COUNT(b) as visitCount " +
      "FROM Booking b " +
      "WHERE b.client.id = :clientId AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED "
      +
      "GROUP BY b.barber.id " +
      "ORDER BY visitCount DESC")
  List<Object[]> findMostVisitedBarbersByClient(@Param("clientId") Long clientId);

  @Query("SELECT b.barber.id, " +
      "COUNT(b) as visitCount, " +
      "MIN(b.startAt) as firstVisit, " +
      "MAX(b.startAt) as lastVisit, " +
      "SUM(b.totalPrice) as totalSpent " +
      "FROM Booking b " +
      "WHERE b.client.id = :clientId AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED "
      +
      "GROUP BY b.barber.id " +
      "ORDER BY visitCount DESC")
  List<Object[]> getBarberVisitStats(@Param("clientId") Long clientId);

  // ===== REVIEWS & CANCEL =====

  @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
      "FROM Booking b WHERE b.id = :bookingId AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED")
  boolean isBookingCompleted(@Param("bookingId") Long bookingId);

  @Query("SELECT b FROM Booking b WHERE b.client.id = :clientId " +
      "AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED AND b.review = null "
      +
      "ORDER BY b.startAt DESC")
  List<Booking> findReviewableBookingsByClient(@Param("clientId") Long clientId,
      @Param("status") BookingStatus status);

  @Query("SELECT b FROM Booking b WHERE b.client.id = :clientId " +
      "AND b.status IN (com.salahtech.BarberShop_Apis.Enums.BookingStatus.CONFIRMED, com.salahtech.BarberShop_Apis.Enums.BookingStatus.PENDING) "
      +
      "AND b.startAt > :now")
  List<Booking> findCancellableBookingsByClient(
      @Param("clientId") Long clientId,
      @Param("now") LocalDateTime now);

  @Query("SELECT COUNT(b) FROM Booking b " +
      "WHERE b.client.id = :clientId AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.CANCELLED")
  Long countCancelledBookingsByClient(@Param("clientId") Long clientId);

  // ===== LOYALTY =====

  @Query("SELECT FLOOR(SUM(b.totalPrice) / 10) FROM Booking b " +
      "WHERE b.client.id = :clientId AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED")
  Integer calculateLoyaltyPointsByClient(@Param("clientId") Long clientId);

  @Query("SELECT COUNT(b) FROM Booking b WHERE b.barber.id = :barberId " +
      "AND b.id != :excludeBookingId " +
      "AND b.status != com.salahtech.BarberShop_Apis.Enums.BookingStatus.CANCELLED " +
      "AND ((b.startAt < :end) AND (b.endAt > :start))")
  Long countConflictingBookingsExcludeId(
      @Param("barberId") Long barberId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("excludeBookingId") Long excludeBookingId);
}