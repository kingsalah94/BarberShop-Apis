package com.salahtech.BarberShop_Apis.reppsitories;

import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.models.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

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
              and b.startAt < :endTime
              and b.endAt   > :startTime
            """)
    List<Booking> findConflictingBookings(@Param("barberId") Long barberId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // ----- “RDV du jour” (utilise un intervalle, portable JPA) -----
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
            @Param("from") LocalDateTime from, // ✅ toujours typé
            @Param("to") LocalDateTime to, // ✅ toujours typé
            Pageable pageable);

    long count(); // déjà hérité de JpaRepository, utile pour totalBookings

    long countByStatus(com.salahtech.BarberShop_Apis.Enums.BookingStatus status);

    // @Query("""
    // select count(b) from Booking b
    // where coalesce(b.startAt, b.bookingDate) >= :from
    // and coalesce(b.startAt, b.bookingDate) < :to
    // """)
    // long countBetween(@Param("from") LocalDateTime from, @Param("to")
    // LocalDateTime to);

    // @Query("""
    // select coalesce(sum(b.totalPrice), 0)
    // from Booking b
    // where b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED
    // """)
    // BigDecimal sumRevenueAllCompleted();

    @Query("""
              select coalesce(sum(b.totalPrice), 0)
              from Booking b
              where b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED
                and coalesce(b.startAt, b.bookingDate) >= :from
                and coalesce(b.startAt, b.bookingDate) <  :to
            """)
    BigDecimal sumRevenueCompletedBetween(@Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // long countByStatus(BookingStatus status);

    @Query("select count(b) from Booking b where b.startAt between :start and :end")
    long countBetween(LocalDateTime start, LocalDateTime end);

    @Query("select sum(b.totalPrice) from Booking b where b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED")
    java.math.BigDecimal sumRevenueAllCompleted();

    // NEW: breakdown par statut sur une période
    @Query("select count(b) from Booking b where b.status = :status and b.startAt between :start and :end")
    long countByStatusBetween(BookingStatus status, LocalDateTime start, LocalDateTime end);

}
