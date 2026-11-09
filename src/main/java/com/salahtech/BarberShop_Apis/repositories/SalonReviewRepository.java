package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.SalonReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalonReviewRepository extends JpaRepository<SalonReview, Long> {
    boolean existsBySalonBooking_Id(Long salonBookingId);
    List<SalonReview> findBySalon_IdOrderByCreatedAtDesc(Long salonId);

    long countBySalon_Id(Long salonId);

    /** Note moyenne d'un salon (null si aucun avis). */
    @Query("SELECT AVG(r.overallRating) FROM SalonReview r WHERE r.salon.id = :salonId")
    Double avgRating(@Param("salonId") Long salonId);
}
