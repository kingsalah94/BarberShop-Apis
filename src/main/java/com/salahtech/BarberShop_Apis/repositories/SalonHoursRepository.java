package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.SalonHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface SalonHoursRepository extends JpaRepository<SalonHours, Long> {
    List<SalonHours> findBySalon_Id(Long salonId);

    /** Plages d'un jour (multi-créneaux), triées par heure d'ouverture. */
    List<SalonHours> findBySalon_IdAndDayOfWeekOrderByOpenAtAsc(Long salonId, DayOfWeek dayOfWeek);

    void deleteBySalon_IdAndDayOfWeek(Long salonId, DayOfWeek dayOfWeek);
}
