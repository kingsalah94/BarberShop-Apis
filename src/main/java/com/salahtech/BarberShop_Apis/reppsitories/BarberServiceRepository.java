package com.salahtech.BarberShop_Apis.reppsitories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.BarberService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.salahtech.BarberShop_Apis.Enums.ServiceCategory;

@Repository
public interface BarberServiceRepository extends JpaRepository<BarberService, Long> {

    List<BarberService> findByBarberId(Long barberId);

    List<BarberService> findByCategory(ServiceCategory category);

    List<BarberService> findByActive(Boolean active);

    @Query("SELECT s FROM BarberService s WHERE s.barber.id = :barberId AND s.active = true")
    List<BarberService> findActiveServicesByBarberId(@Param("barberId") Long barberId);

    @Query("SELECT s FROM BarberService s WHERE s.category = :category AND s.active = true")
    List<BarberService> findActiveByCategoryOrderByPriceAsc(@Param("category") ServiceCategory category);

    @Query("SELECT s FROM BarberService s WHERE s.price >= :minPrice AND s.price <= :maxPrice AND s.active = true")
    List<BarberService> findByPriceRangeAndActive(@Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT s FROM BarberService s WHERE s.duration <= :maxDuration AND s.active = true")
    List<BarberService> findByMaxDurationAndActive(@Param("maxDuration") Integer maxDuration);

    @Query("SELECT s FROM BarberService s WHERE s.name LIKE %:name% AND s.active = true")
    List<BarberService> findByNameContainingAndActive(@Param("name") String name);

    @Query("SELECT DISTINCT s.category FROM BarberService s WHERE s.active = true")
    List<ServiceCategory> findDistinctActiveCategories();

    Page<BarberService> findAll(Specification<BarberService> spec, Pageable pageable);

    // ===== NOUVELLES MÉTHODES POUR LES STATISTIQUES =====

    /**
     * Compte le nombre total de réservations pour un service donné
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.barberService.id = :serviceId")
    Long countBookingsByServiceId(@Param("serviceId") Long serviceId);

    /**
     * Calcule le revenu total généré par un service (réservations complétées
     * uniquement)
     */
    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b " +
            "WHERE b.barberService.id = :serviceId " +
            "AND b.status = com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED")
    BigDecimal calculateServiceRevenue(@Param("serviceId") Long serviceId);

    /**
     * Calcule la note moyenne d'un service basée sur les avis des réservations
     */
    @Query("SELECT COALESCE(AVG(b.rating), 0.0) FROM Booking b " +
            "WHERE b.barberService.id = :serviceId " +
            "AND b.rating IS NOT NULL")
    Double calculateServiceRating(@Param("serviceId") Long serviceId);
}
