package com.salahtech.BarberShop_Apis.repositories;

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

        /** Barbiers rattachés à un salon (tenant) */
        List<Barber> findBySalon_Id(Long salonId);

        long countBySalon_Id(Long salonId);

        boolean existsByUser_Id(Long userId);

        /** Barbier (modèle legacy) rattaché à ce salon ? Utilisé par TenantGuard. */
        boolean existsByUser_IdAndSalon_Id(Long userId, Long salonId);

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

        // Formule Haversine (6371 = rayon de la terre en km)
        // Retourne les barbiers triés par distance
        @Query(value = """
                        SELECT *,
                        (6371 * acos(cos(radians(:userLat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:userLng)) + sin(radians(:userLat)) * sin(radians(latitude)))) AS distance
                        FROM users
                        WHERE dtype = 'Barber'
                        HAVING distance < :radiusKm
                        ORDER BY distance ASC
                        """, nativeQuery = true)
        List<Barber> findBarbersNearby(@Param("userLat") double userLat,
                        @Param("userLng") double userLng,
                        @Param("radiusKm") double radiusKm);


        /** Salon de rattachement d'un barbier (contrôle d'accès multi-tenant). */
        @org.springframework.data.jpa.repository.Query("SELECT s.id FROM Barber b JOIN b.salon s WHERE b.id = :id")
        java.util.Optional<Long> findSalonIdById(@Param("id") Long id);
}
