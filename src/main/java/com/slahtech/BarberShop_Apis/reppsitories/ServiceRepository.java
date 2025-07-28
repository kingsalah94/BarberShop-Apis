package com.slahtech.BarberShop_Apis.reppsitories;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.slahtech.BarberShop_Apis.Enums.ServiceCategory;
import com.slahtech.BarberShop_Apis.models.Service;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
    
    List<Service> findByBarberId(Long barberId);
    
    List<Service> findByCategory(ServiceCategory category);
    
    List<Service> findByActive(Boolean active);
    
    @Query("SELECT s FROM Service s WHERE s.barber.id = :barberId AND s.active = true")
    List<Service> findActiveServicesByBarberId(@Param("barberId") Long barberId);
    
    @Query("SELECT s FROM Service s WHERE s.category = :category AND s.active = true")
    List<Service> findActiveByCategoryOrderByPriceAsc(@Param("category") ServiceCategory category);
    
    @Query("SELECT s FROM Service s WHERE s.price >= :minPrice AND s.price <= :maxPrice AND s.active = true")
    List<Service> findByPriceRangeAndActive(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    @Query("SELECT s FROM Service s WHERE s.duration <= :maxDuration AND s.active = true")
    List<Service> findByMaxDurationAndActive(@Param("maxDuration") Integer maxDuration);
    
    @Query("SELECT s FROM Service s WHERE s.name LIKE %:name% AND s.active = true")
    List<Service> findByNameContainingAndActive(@Param("name") String name);
    
    @Query("SELECT DISTINCT s.category FROM Service s WHERE s.active = true")
    List<ServiceCategory> findDistinctActiveCategories();
}
