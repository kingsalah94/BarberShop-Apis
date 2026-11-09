package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Find all reviews for a specific barber (excluding deleted)
     */
    // Dans ReviewRepository
    @Query("SELECT r FROM Review r WHERE r.barber.id = :barberId AND r.isDeleted = false ORDER BY r.createdAt DESC")
    Page<Review> findByBarberId(@Param("barberId") Long barberId, Pageable pageable);

    /**
     * Find all reviews by a specific client (excluding deleted)
     */
    @Query("SELECT r FROM Review r WHERE r.client.id = :clientId AND r.isDeleted = false ORDER BY r.createdAt DESC")
    Page<Review> findByClientId(@Param("clientId") Long clientId, Pageable pageable);

    /**
     * Find review for a specific booking
     */
    Optional<Review> findByBookingIdAndIsDeletedFalse(Long bookingId);

    /**
     * Count reviews for a barber
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.barber.id = :barberId AND r.isDeleted = false")
    Long countByBarberId(@Param("barberId") Long barberId);

    /**
     * Count reviews by a client
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.client.id = :clientId AND r.isDeleted = false")
    Long countByClientId(@Param("clientId") Long clientId);

    /**
     * Get average rating for a barber
     */
    @Query("SELECT AVG(r.overallRating) FROM Review r WHERE r.barber.id = :barberId AND r.isDeleted = false")
    Double getAverageRatingForBarber(@Param("barberId") Long barberId);

    /**
     * Get reviews with rating >= minRating
     */
    @Query("SELECT r FROM Review r WHERE r.barber.id = :barberId AND r.overallRating >= :minRating AND r.isDeleted = false")
    List<Review> findByBarberIdAndMinRating(@Param("barberId") Long barberId, @Param("minRating") Integer minRating);

    /**
     * Find recent reviews for a barber (last N days)
     */
    @Query("SELECT r FROM Review r WHERE r.barber.id = :barberId AND r.createdAt >= CURRENT_TIMESTAMP - :days DAY AND r.isDeleted = false ORDER BY r.createdAt DESC")
    List<Review> findRecentReviewsForBarber(@Param("barberId") Long barberId, @Param("days") Integer days);

    /**
     * Check if client has already reviewed a booking
     */
    boolean existsByBookingIdAndClientIdAndIsDeletedFalse(Long bookingId, Long clientId);

    Collection<String> findByBarberIdOrderByCreatedAtDesc(Long barberId, PageRequest of);
}
