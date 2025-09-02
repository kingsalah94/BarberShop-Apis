package com.salahtech.BarberShop_Apis.reppsitories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // <<< CORRECT
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.salahtech.BarberShop_Apis.models.Barber;

@Repository
public interface BarberRepository extends JpaRepository<Barber, Long>, JpaSpecificationExecutor<Barber> {

        Optional<Barber> findByUserId(Long userId);

        boolean existsByUserId(Long userId);

        boolean existsByUserIdAndIdNot(Long userId, Long id);

        // Non paginé (utilisé par findAllAvailable())
        List<Barber> findByAvailable(boolean available);

        // ---------- PAGINÉ/TRIABLE ----------
        Page<Barber> findByLocationContaining(String location, Pageable pageable);

        Page<Barber> findBySalonNameContaining(String salonName, Pageable pageable);

        @Query("select b from Barber b where b.priceFrom between :min and :max")
        Page<Barber> findByPriceRange(@Param("min") BigDecimal min,
                        @Param("max") BigDecimal max,
                        Pageable pageable);

        @Query("select b from Barber b where b.rating >= :minRating order by b.rating desc")
        Page<Barber> findByMinRatingOrderByRatingDesc(@Param("minRating") BigDecimal minRating,
                        Pageable pageable);

        @Query("select b from Barber b where b.available = true and b.location like %:location%")
        Page<Barber> findAvailableByLocation(@Param("location") String location, Pageable pageable);

        // ---------- NON PAGINÉ ----------

        // List<Barber> findByAvailable(Boolean available);

        // @Query("SELECT b FROM Barber b WHERE b.location LIKE %:location%")
        // List<Barber> findByLocationContaining(@Param("location") String location);

        // @Query("SELECT b FROM Barber b WHERE b.salonName LIKE %:salonName%")
        // List<Barber> findBySalonNameContaining(@Param("salonName") String salonName);

        // @Query("SELECT b FROM Barber b WHERE b.priceFrom >= :minPrice AND b.priceFrom
        // <= :maxPrice")
        // List<Barber> findByPriceRange(@Param("minPrice") BigDecimal minPrice,
        // @Param("maxPrice") BigDecimal maxPrice);

        @Query("SELECT b FROM Barber b WHERE b.rating >= :minRating ORDER BY b.rating DESC")
        List<Barber> findByMinRatingOrderByRatingDesc(@Param("minRating") BigDecimal minRating);

        // @Query("SELECT b FROM Barber b WHERE b.available = true AND b.location LIKE
        // %:location%")
        // List<Barber> findAvailableByLocation(@Param("location") String location);

        @Query("SELECT b FROM Barber b ORDER BY b.rating DESC")
        List<Barber> findAllOrderByRatingDesc();

        // boolean existsByUserId(Long userId);
}
