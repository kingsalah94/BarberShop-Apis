package com.salahtech.BarberShop_Apis.reppsitories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.models.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByUserId(Long userId);
    
    List<Booking> findByBarberId(Long barberId);
    
    List<Booking> findByBarberServiceId(Long serviceId);
    
    List<Booking> findByStatus(BookingStatus status);
    
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.bookingDate DESC")
    List<Booking> findByUserIdOrderByBookingDateDesc(@Param("userId") Long userId);
    
    @Query("SELECT b FROM Booking b WHERE b.barber.id = :barberId ORDER BY b.bookingDate ASC")
    List<Booking> findByBarberIdOrderByBookingDateAsc(@Param("barberId") Long barberId);
    
    @Query("SELECT b FROM Booking b WHERE b.bookingDate BETWEEN :startDate AND :endDate")
    List<Booking> findByBookingDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT b FROM Booking b WHERE b.barber.id = :barberId AND b.bookingDate BETWEEN :startDate AND :endDate AND b.status IN ('CONFIRMED', 'PENDING')")
    List<Booking> findConflictingBookings(@Param("barberId") Long barberId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.bookingDate >= :fromDate ORDER BY b.bookingDate ASC")
    List<Booking> findByStatusAndBookingDateAfterOrderByBookingDateAsc(@Param("status") BookingStatus status, @Param("fromDate") LocalDateTime fromDate);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.barber.id = :barberId AND b.status = 'COMPLETED'")
    Long countCompletedBookingsByBarberId(@Param("barberId") Long barberId);
}
