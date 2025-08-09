package com.salahtech.BarberShop_Apis.reppsitories;


import com.salahtech.BarberShop_Apis.models.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    // Trouver toutes les disponibilités d'un barber
    List<Availability> findByBarberId(Long barberId);

    // Trouver les disponibilités d'un barber pour un jour précis
    List<Availability> findByBarberIdAndDayOfWeek(Long barberId, DayOfWeek dayOfWeek);

    // Supprimer toutes les disponibilités d'un barber
    void deleteByBarberId(Long barberId);

    // Trouver les disponibilités dans une plage de dates (si besoin futur)
    List<Availability> findByBarberIdAndDateBetween(Long barberId, LocalDateTime startDate, LocalDateTime endDate);
}
