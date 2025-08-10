package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.AvailabilityDto;
import com.salahtech.BarberShop_Apis.Dtos.TimeSlotDto;
import com.salahtech.BarberShop_Apis.Services.Implementations.AvailabilityServiceImpl.AvailabilityStatsDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AvailabilityService extends BaseService<AvailabilityDto, Long> {
    
    /*
     * Obtenir les créneaux horaires disponibles pour un barbier
     * pour une date donnée, en tenant compte de la durée du service
     */
    List<TimeSlotDto> getAvailableSlots(Long barberId, LocalDate date, Integer serviceDuration);
    
    /*
     * Vérifier si un créneau horaire est disponible pour un barbier
     * entre deux dates (startTime et endTime).
     */
    boolean isSlotAvailable(Long barberId, LocalDateTime startTime, LocalDateTime endTime);
    
    /*
     * Bloquer ou libérer un créneau horaire pour un barbier
     * avec une raison optionnelle (par exemple, pour des vacances ou un événement spécial).
     */
    void blockTimeSlot(Long barberId, LocalDateTime startTime, LocalDateTime endTime, String reason);
    
    /*
     * Libérer un créneau horaire précédemment bloqué
     * (par exemple, si le barbier revient de vacances ou si l'événement est annulé).
     */
    void releaseTimeSlot(Long barberId, LocalDateTime startTime, LocalDateTime endTime);
    
    /*
     * Obtenir la disponibilité hebdomadaire d'un barbier
     * pour une semaine donnée, en renvoyant une liste de créneaux horaires.
     */
    List<AvailabilityDto> getWeeklyAvailability(Long barberId, LocalDate weekStart);
    
    /*
     * Mettre à jour la disponibilité hebdomadaire d'un barbier
     * en fournissant une liste de créneaux horaires pour chaque jour de la semaine.
     */
    void updateWeeklySchedule(Long barberId, List<AvailabilityDto> weeklySchedule);

    /**
     * Obtenir les statistiques de disponibilité d'un barbier
     */
    AvailabilityStatsDTO getAvailabilityStats(Long barberId, LocalDate startDate, LocalDate endDate);

    /**
     * Générer les créneaux horaires pour une date donnée
     */
    List<TimeSlotDto> generateTimeSlots(LocalDate date, String workingHours);

    /**
     * Obtenir la disponibilité d'un barbier pour une date donnée
     */
    AvailabilityDto getBarberAvailability(Long barberId, LocalDate date);

    /**
     * Obtenir les prochains créneaux disponibles pour un barbier
     */
    List<TimeSlotDto> getNextAvailableSlots(Long barberId, int numberOfSlots);

    /**
     * Obtenir la disponibilité de tous les barbiers pour une date
     */
    List<AvailabilityDto> getAllBarbersAvailability(LocalDate date);

    /**
     * Vérifier si un créneau est disponible
     */
    boolean isTimeSlotAvailable(Long barberId, LocalDateTime startTime, LocalDateTime endTime);
}
