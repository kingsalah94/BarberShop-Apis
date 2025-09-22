package com.salahtech.BarberShop_Apis.reppsitories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.salahtech.BarberShop_Apis.models.Barber;

@Repository
public interface BarberRepository extends JpaRepository<Barber, Long>, JpaSpecificationExecutor<Barber> {

        /** ✅ relation OneToOne: utiliser user.id */
        Optional<Barber> findByUser_Id(Long userId);

        boolean existsByUser_Id(Long userId);

        boolean existsByUser_IdAndIdNot(Long userId, Long id);

        // ---------- PAGINÉ/TRIABLE ----------
        List<Barber> findByAvailable(boolean available);

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
        @Query("SELECT b FROM Barber b WHERE b.rating >= :minRating ORDER BY b.rating DESC")
        List<Barber> findByMinRatingOrderByRatingDesc(@Param("minRating") BigDecimal minRating);

        @Query("SELECT b FROM Barber b ORDER BY b.rating DESC")
        List<Barber> findAllOrderByRatingDesc();

        @Query("SELECT b FROM Barber b WHERE b.available = true")
        List<Barber> findAllAvailable();

        @Query("SELECT b FROM Barber b WHERE b.available = true AND b.location LIKE %:location%")
        List<Barber> findAllAvailableByLocation(@Param("location") String location);

        boolean existsByUserIdAndIdNot(Long resolvedUserId, Long id);

        // Dans BarberRepository, ajouter des fetch joins
        @Query("SELECT b FROM Barber b LEFT JOIN FETCH b.user WHERE b.id = :id")
        Optional<Barber> findByIdWithUser(@Param("id") Long id);

        // @EntityGraph(attributePaths = { "user", "barberServices" })
        // List<Barber> findAllWithDetails();
}
