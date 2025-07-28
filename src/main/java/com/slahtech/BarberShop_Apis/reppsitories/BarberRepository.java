package com.slahtech.BarberShop_Apis.reppsitories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.slahtech.BarberShop_Apis.models.Barber;

@Repository
public interface BarberRepository extends JpaRepository<Barber, Long> {
    
    Optional<Barber> findByUserId(Long userId);
    
    List<Barber> findByAvailable(Boolean available);
    
    @Query("SELECT b FROM Barber b WHERE b.location LIKE %:location%")
    List<Barber> findByLocationContaining(@Param("location") String location);
    
    @Query("SELECT b FROM Barber b WHERE b.salonName LIKE %:salonName%")
    List<Barber> findBySalonNameContaining(@Param("salonName") String salonName);
    
    @Query("SELECT b FROM Barber b WHERE b.priceFrom >= :minPrice AND b.priceFrom <= :maxPrice")
    List<Barber> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    @Query("SELECT b FROM Barber b WHERE b.rating >= :minRating ORDER BY b.rating DESC")
    List<Barber> findByMinRatingOrderByRatingDesc(@Param("minRating") BigDecimal minRating);
    
    @Query("SELECT b FROM Barber b WHERE b.available = true AND b.location LIKE %:location%")
    List<Barber> findAvailableByLocation(@Param("location") String location);
    
    @Query("SELECT b FROM Barber b ORDER BY b.rating DESC")
    List<Barber> findAllOrderByRatingDesc();
}
