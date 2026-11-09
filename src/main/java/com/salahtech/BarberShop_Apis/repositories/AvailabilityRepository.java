package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

        // ✅ GARDEZ celle-ci - bon type de paramètre
        List<Availability> findByBarber_IdAndDayOfWeek(Long barberId, DayOfWeek dayOfWeek);

        // Ou utilisez @Query pour être plus explicite
        @Query("SELECT a FROM Availability a WHERE a.barber.id = :barberId AND a.dayOfWeek = :dayOfWeek")
        List<Availability> findByBarberIdAndDayOfWeek(
                        @Param("barberId") Long barberId,
                        @Param("dayOfWeek") DayOfWeek dayOfWeek);

        // Pour les disponibilités ponctuelles (par date)
        List<Availability> findByBarber_IdAndDate(Long barberId, LocalDate date);

        // Alternative avec @Query
        @Query("SELECT a FROM Availability a WHERE a.barber.id = :barberId AND a.date = :date")
        List<Availability> findByBarberIdAndDate(
                        @Param("barberId") Long barberId,
                        @Param("date") LocalDate date);

        // Pour recherche entre deux dates/heures
        List<Availability> findByBarber_IdAndDateBetween(Long barberId, LocalDateTime startDate, LocalDateTime endDate);

        // Alternative sans underscore
        // --- PLAGE DE DATES (Correction importante) ---
        // Cette méthode est utilisée par getAvailabilityStats et getAvailableSlots
        // On utilise LocalDate car l'entité stocke une date, pas une date+heure
        @Query("SELECT a FROM Availability a WHERE a.barber.id = :barberId AND a.date BETWEEN :startAt AND :endAt")
        List<Availability> findByBarberIdAndDateBetween(
                        @Param("barberId") Long barberId,
                        @Param("startAt") LocalDateTime startAt,
                        @Param("endAt") LocalDateTime endAt);

        // Liste par barbier
        List<Availability> findByBarber_Id(Long barberId);

        List<Availability> findByBarberId(Long barberId);

        // Supprimer toutes les disponibilités d'un barbier
        void deleteByBarber_Id(Long barberId);

        // Liste complète triée
        @Query("""
                            SELECT a FROM Availability a
                            WHERE a.barber.id = :barberId
                            ORDER BY a.date NULLS LAST, a.dayOfWeek ASC, a.startAt ASC
                        """)
        List<Availability> findAllByBarberSorted(@Param("barberId") Long barberId);

        /**
         * Trouver toutes les disponibilités d'un barbier qui ont une date (exceptions
         * ponctuelles)
         * 
         * @param barberId ID du barbier
         * @return Liste des exceptions
         */
        List<Availability> findByBarberIdAndDateIsNotNull(Long barberId);

        List<Availability> findByBarberIdAndDateIsNull(Long barberId);

    /** Barbier propriétaire du créneau/exception (contrôle d'accès). */
    @org.springframework.data.jpa.repository.Query("SELECT b.id FROM Availability a JOIN a.barber b WHERE a.id = :id")
    java.util.Optional<Long> findBarberIdById(@org.springframework.data.repository.query.Param("id") Long id);
}