package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.SalonOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalonOfferingRepository extends JpaRepository<SalonOffering, Long> {
    List<SalonOffering> findBySalon_IdOrderByNameAsc(Long salonId);
    List<SalonOffering> findBySalon_IdAndActiveTrueOrderByNameAsc(Long salonId);
    long countBySalon_IdAndActiveTrue(Long salonId);
}
