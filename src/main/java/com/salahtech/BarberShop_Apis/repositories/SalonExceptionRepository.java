package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.SalonException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalonExceptionRepository extends JpaRepository<SalonException, Long> {
    List<SalonException> findBySalon_IdOrderByDateAsc(Long salonId);
    List<SalonException> findBySalon_IdAndDateGreaterThanEqualOrderByDateAsc(Long salonId, LocalDate from);
    Optional<SalonException> findBySalon_IdAndDate(Long salonId, LocalDate date);
}
