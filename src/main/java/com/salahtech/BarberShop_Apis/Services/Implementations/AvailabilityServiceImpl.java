package com.salahtech.BarberShop_Apis.Services.Implementations;


import com.salahtech.BarberShop_Apis.Dtos.AvailabilityDto;
import com.salahtech.BarberShop_Apis.Dtos.TimeSlotDto;
import com.salahtech.BarberShop_Apis.Enums.ErrorCodes;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.AvailabilityService;
import com.salahtech.BarberShop_Apis.models.Availability;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.reppsitories.AvailabilityRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AvailabilityServiceImpl implements AvailabilityService {
    
    private final AvailabilityRepository availabilityRepository;
    private final BookingRepository bookingRepository;
    
    @Override
    public AvailabilityDto save(AvailabilityDto dto) {
        Availability availability = AvailabilityDto.toEntity(dto);
        return AvailabilityDto.fromEntity(availabilityRepository.save(availability));
    }
    
    @Override
    public AvailabilityDto findById(Long id) {
        return availabilityRepository.findById(id)
                .map(AvailabilityDto::fromEntity)
                .orElseThrow(() -> new InvalideEntityException(
                        "Aucune disponibilité trouvée avec l'ID " + id,
                        ErrorCodes.AVAILABILITY_NOT_FOUND));
    }
    
    @Override
    public List<AvailabilityDto> findAll() {
        return availabilityRepository.findAll().stream()
                .map(AvailabilityDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public void delete(Long id) {
        if (id == null) {
            log.error("ID de disponibilité est null");
            return;
        }
        availabilityRepository.deleteById(id);
    }

    @Override
public List<TimeSlotDto> getAvailableSlots(Long barberId, LocalDate date, Integer serviceDuration) {
    log.info("Recherche des créneaux disponibles pour le barbier {} le {}", barberId, date);

    if (barberId == null || date == null || serviceDuration == null || serviceDuration <= 0) {
        log.warn("Paramètres invalides: barberId={}, date={}, serviceDuration={}", barberId, date, serviceDuration);
        return Collections.emptyList();
    }

    // 1) Dispos du jour
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    List<Availability> dailyAvailabilities =
            availabilityRepository.findByBarberIdAndDayOfWeek(barberId, dayOfWeek);
    if (dailyAvailabilities.isEmpty()) {
        log.info("Aucune disponibilité définie pour le barbier {} le {}", barberId, dayOfWeek);
        return Collections.emptyList();
    }

    // 2) Bookings existants du jour (on ignore CANCELLED)
    LocalDateTime startOfDay = date.atStartOfDay();
    LocalDateTime endOfDay   = date.atTime(23, 59, 59);
    List<Booking> existingBookings = bookingRepository
            .findConflictingBookings(barberId, startOfDay, endOfDay)
            .stream()
            .filter(b -> b.getStatus() == null
                    || "PENDING".equals(b.getStatus().name())
                    || "CONFIRMED".equals(b.getStatus().name()))
            .toList();

    List<TimeSlotDto> availableSlots = new ArrayList<>();

    // 3) Pour chaque plage de dispo
    for (Availability availability : dailyAvailabilities) {
        LocalDateTime slotStart = date.atTime(availability.getStartTime());
        LocalDateTime availEnd  = date.atTime(availability.getEndTime());

        while (true) {
            LocalDateTime slotEnd = slotStart.plusMinutes(serviceDuration);
            if (slotEnd.isAfter(availEnd)) break;

            // Copie finale pour la lambda
            final LocalDateTime checkStart = slotStart;
            final LocalDateTime checkEnd   = slotEnd;

            boolean overlaps = existingBookings.stream().anyMatch(b -> {
                LocalDateTime bStart = b.getStartAt();
                LocalDateTime bEnd   = b.getEndAt();
                return bStart != null && bEnd != null
                        && checkStart.isBefore(bEnd)
                        && checkEnd.isAfter(bStart);
            });

            if (!overlaps) {
                availableSlots.add(TimeSlotDto.builder()
                        .startTime(slotStart)
                        .endTime(slotEnd)
                        .available(true)
                        .build());
            }

            // Avancer de 30 min
            slotStart = slotStart.plusMinutes(30);
        }
    }

    log.info("Trouvé {} créneaux disponibles", availableSlots.size());
    return availableSlots;
}


    
    // @Override
    // public List<TimeSlotDto> getAvailableSlots(Long barberId, LocalDate date, Integer serviceDuration) {
    //     log.info("Recherche des créneaux disponibles pour le barbier {} le {}", barberId, date);
        
    //     // Récupérer les disponibilités du barbier pour ce jour
    //     DayOfWeek dayOfWeek = date.getDayOfWeek();
    //     List<Availability> dailyAvailabilities = availabilityRepository
    //             .findByBarberIdAndDayOfWeek(barberId, dayOfWeek);
        
    //     if (dailyAvailabilities.isEmpty()) {
    //         log.info("Aucune disponibilité définie pour le barbier {} le {}", barberId, dayOfWeek);
    //         return new ArrayList<>();
    //     }
        
    //     // Récupérer les réservations existantes pour ce jour
    //     LocalDateTime startOfDay = date.atStartOfDay();
    //     LocalDateTime endOfDay = date.atTime(23, 59, 59);
    //     List<Booking> existingBookings = bookingRepository
    //             .findConflictingBookings(barberId, startOfDay, endOfDay);
        
    //     List<TimeSlotDto> availableSlots = new ArrayList<>();
        
    //     for (Availability availability : dailyAvailabilities) {
    //         LocalDateTime currentSlot = date.atTime(availability.getStartTime());
    //         LocalDateTime endTime = date.atTime(availability.getEndTime());
            
    //         while (currentSlot.plusMinutes(serviceDuration).isBefore(endTime) || 
    //                currentSlot.plusMinutes(serviceDuration).isEqual(endTime)) {
                
    //             LocalDateTime slotEnd = currentSlot.plusMinutes(serviceDuration);
                
    //             // Vérifier si le créneau est libre
    //             // boolean isSlotFree = existingBookings.stream()
    //             //         .noneMatch(booking -> 
    //             //             (currentSlot.isBefore(booking.getEndTime()) && 
    //             //              slotEnd.isAfter(booking.getBookingTime())));
    //             boolean isSlotFree = existingBookings.stream().noneMatch(b -> {
    //             LocalDateTime bStart = b.getStartAt();
    //             LocalDateTime bEnd   = b.getEndAt();
    //             return bStart != null && bEnd != null
    //                 && currentSlot.isBefore(bEnd)
    //                 && slotEnd.isAfter(bStart);
    //         });

                
    //             if (isSlotFree) {
    //                 availableSlots.add(TimeSlotDto.builder()
    //                         .startTime(currentSlot)
    //                         .endTime(slotEnd)
    //                         .available(true)
    //                         .build());
    //             }
                
    //             // Passer au créneau suivant (par intervalles de 30 minutes)
    //             currentSlot = currentSlot.plusMinutes(30);
    //         }
    //     }
        
    //     log.info("Trouvé {} créneaux disponibles", availableSlots.size());
    //     return availableSlots;
    // }
    
    @Override
    public boolean isSlotAvailable(Long barberId, LocalDateTime startTime, LocalDateTime endTime) {
        // Vérifier les disponibilités du barbier
        DayOfWeek dayOfWeek = startTime.getDayOfWeek();
        List<Availability> dailyAvailabilities = availabilityRepository
                .findByBarberIdAndDayOfWeek(barberId, dayOfWeek);
        
        boolean withinAvailableHours = dailyAvailabilities.stream()
                .anyMatch(availability -> 
                    !startTime.toLocalTime().isBefore(availability.getStartTime()) &&
                    !endTime.toLocalTime().isAfter(availability.getEndTime()));
        
        if (!withinAvailableHours) {
            return false;
        }
        
        // Vérifier les conflits avec les réservations existantes
        List<Booking> conflictingBookings = bookingRepository
                .findConflictingBookings(barberId, startTime, endTime);
        
        return conflictingBookings.isEmpty();
    }
    
    @Override
    public void blockTimeSlot(Long barberId, LocalDateTime startTime, LocalDateTime endTime, String reason) {
        log.info("Blocage du créneau pour le barbier {} de {} à {} - Raison: {}", 
                barberId, startTime, endTime, reason);
        
        // Créer une réservation de blocage (avec un statut spécial)
        // Ou utiliser une entité BlockedSlot séparée
        // Pour simplifier, on peut créer une disponibilité négative
    }
    
    @Override
    public void releaseTimeSlot(Long barberId, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("Libération du créneau pour le barbier {} de {} à {}", 
                barberId, startTime, endTime);
        // Supprimer les blocages pour cette période
    }
    
    @Override
    public List<AvailabilityDto> getWeeklyAvailability(Long barberId, LocalDate weekStart) {
        return availabilityRepository.findByBarberId(barberId).stream()
                .map(AvailabilityDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateWeeklySchedule(Long barberId, List<AvailabilityDto> weeklySchedule) {
        log.info("Mise à jour de l'horaire hebdomadaire pour le barbier {}", barberId);
        
        // Supprimer l'ancien horaire
        availabilityRepository.deleteByBarberId(barberId);
        
        // Sauvegarder le nouveau
        List<Availability> availabilities = weeklySchedule.stream()
                .map(dto -> {
                    Availability availability = AvailabilityDto.toEntity(dto);
                    availability.getBarber().setId(barberId);
                    return availability;
                })
                .collect(Collectors.toList());
        
        availabilityRepository.saveAll(availabilities);
    }
}
