package com.salahtech.BarberShop_Apis.Services.Implementations;


import com.salahtech.BarberShop_Apis.Dtos.AvailabilityDto;
import com.salahtech.BarberShop_Apis.Dtos.TimeSlotDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Enums.ErrorCodes;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.AvailabilityService;
import com.salahtech.BarberShop_Apis.models.Availability;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.reppsitories.AvailabilityRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BarberRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
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
     @Autowired
    private BarberRepository barberRepository;

    // Horaires de travail par défaut
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(18, 0);
    private static final int DEFAULT_SLOT_DURATION = 30; // minutes
    
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

      /**
     * Obtenir la disponibilité d'un barbier pour une date donnée
     */
    @Override
    public AvailabilityDto getBarberAvailability(Long barberId, LocalDate date) {
        log.info("Getting availability for barber {} on date {}", barberId, date);

        Barber barber = barberRepository.findById(barberId)
            .orElseThrow(() -> new ResourceNotFoundException("Barber not found with id: " + barberId));

        if (!barber.getAvailable()) {
            log.info("Barber {} is not available", barberId);
            return new AvailabilityDto(barberId, date.atStartOfDay(), new ArrayList<>(), new ArrayList<>());
        }

        // Obtenir les réservations existantes pour cette date
        LocalDateTime startOfDay = date.atTime(LocalTime.MIN);
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        List<Booking> bookings = bookingRepository.findByBookingDateBetween(startOfDay, endOfDay)
            .stream()
            .filter(booking -> booking.getBarber().getId().equals(barberId))
            .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED || 
                             booking.getStatus() == BookingStatus.PENDING)
            .collect(Collectors.toList());

        // Générer tous les créneaux possibles
        List<TimeSlotDto> allSlots = generateTimeSlots(date, barber.getWorkingHours());
        
        // Marquer les créneaux occupés
        List<TimeSlotDto> availableSlots = new ArrayList<>();
        List<TimeSlotDto> bookedSlots = new ArrayList<>();

        for (TimeSlotDto slot : allSlots) {
            boolean isBooked = bookings.stream().anyMatch(booking -> 
                isTimeSlotOverlapping(slot.getStartTime(), slot.getEndTime(), 
                                    booking.getBookingDate(), booking.getEndTime()));

            if (isBooked) {
                Booking overlappingBooking = bookings.stream()
                    .filter(booking -> isTimeSlotOverlapping(slot.getStartTime(), slot.getEndTime(), 
                                                           booking.getBookingDate(), booking.getEndTime()))
                    .findFirst()
                    .orElse(null);
                
                slot.setAvailable(false);
                slot.setBookingId(overlappingBooking != null ? overlappingBooking.getId() : null);
                bookedSlots.add(slot);
            } else {
                slot.setAvailable(true);
                availableSlots.add(slot);
            }
        }

        log.info("Found {} available slots and {} booked slots for barber {} on {}", 
                availableSlots.size(), bookedSlots.size(), barberId, date);

        return new AvailabilityDto(barberId, date.atStartOfDay(), availableSlots, bookedSlots);
    }


    /**
     * Vérifier si un créneau est disponible
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isTimeSlotAvailable(Long barberId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
            barberId, startTime, endTime);
        
        return conflictingBookings.isEmpty();
    }

    /**
     * Obtenir les prochains créneaux disponibles pour un barbier
     */
    @Override
    public List<TimeSlotDto> getNextAvailableSlots(Long barberId, int numberOfSlots) {
        log.info("Getting next {} available slots for barber {}", numberOfSlots, barberId);

        Barber barber = barberRepository.findById(barberId)
            .orElseThrow(() -> new ResourceNotFoundException("Barber not found with id: " + barberId));

        if (!barber.getAvailable()) {
            return new ArrayList<>();
        }

        List<TimeSlotDto> availableSlots = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        int daysToCheck = 30; // Chercher sur les 30 prochains jours

        for (int i = 0; i < daysToCheck && availableSlots.size() < numberOfSlots; i++) {
            LocalDate dateToCheck = currentDate.plusDays(i);
            AvailabilityDto availability = getBarberAvailability(barberId, dateToCheck);
            
            List<TimeSlotDto> dailyAvailable = availability.getAvailableSlots().stream()
                .filter(slot -> slot.getStartTime().isAfter(LocalDateTime.now()))
                .limit(numberOfSlots - availableSlots.size())
                .collect(Collectors.toList());
            
            availableSlots.addAll(dailyAvailable);
        }

        log.info("Found {} next available slots for barber {}", availableSlots.size(), barberId);
        return availableSlots;
    }

     /**
     * Obtenir la disponibilité de tous les barbiers pour une date
     */
    @Override
    public List<AvailabilityDto> getAllBarbersAvailability(LocalDate date) {
        List<Barber> activeBarbers = barberRepository.findByAvailable(true);
        
        return activeBarbers.stream()
            .map(barber -> getBarberAvailability(barber.getId(), date))
            .collect(Collectors.toList());
    }

      /**
     * Générer les créneaux horaires pour une date donnée
     */
    @Override
    public List<TimeSlotDto> generateTimeSlots(LocalDate date, String workingHours) {
        List<TimeSlotDto> slots = new ArrayList<>();
        
        // Parser les heures de travail (format simple pour l'exemple)
        // Format attendu: "09:00-18:00" ou utiliser les heures par défaut
        LocalTime startTime = DEFAULT_START_TIME;
        LocalTime endTime = DEFAULT_END_TIME;
        
        if (workingHours != null && workingHours.contains("-")) {
            String[] hours = workingHours.split("-");
            if (hours.length == 2) {
                try {
                    startTime = LocalTime.parse(hours[0].trim());
                    endTime = LocalTime.parse(hours[1].trim());
                } catch (Exception e) {
                    log.warn("Could not parse working hours: {}. Using default.", workingHours);
                }
            }
        }

        // Générer les créneaux
        LocalDateTime currentSlot = date.atTime(startTime);
        LocalDateTime endOfDay = date.atTime(endTime);

        while (currentSlot.isBefore(endOfDay)) {
            LocalDateTime slotEnd = currentSlot.plusMinutes(DEFAULT_SLOT_DURATION);
            
            if (slotEnd.isAfter(endOfDay)) {
                break;
            }

            TimeSlotDto timeSlot = new TimeSlotDto();
            timeSlot.setStartTime(currentSlot);
            timeSlot.setEndTime(slotEnd);
            timeSlot.setAvailable(true);

            slots.add(timeSlot);
            currentSlot = currentSlot.plusMinutes(DEFAULT_SLOT_DURATION);
        }

        return slots;
    }

    /**
     * Vérifier si deux créneaux se chevauchent
     */

    private boolean isTimeSlotOverlapping(LocalDateTime slot1Start, LocalDateTime slot1End,
                                        LocalDateTime slot2Start, LocalDateTime slot2End) {
        return slot1Start.isBefore(slot2End) && slot2Start.isBefore(slot1End);
    }


    /**
     * Obtenir les statistiques de disponibilité d'un barbier
     */
    @Override
    public AvailabilityStatsDTO getAvailabilityStats(Long barberId, LocalDate startDate, LocalDate endDate) {
        log.info("Getting availability stats for barber {} from {} to {}", barberId, startDate, endDate);

        int totalDays = (int) startDate.datesUntil(endDate.plusDays(1)).count();
        int totalSlots = 0;
        int bookedSlots = 0;

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            AvailabilityDto availability = getBarberAvailability(barberId, currentDate);
            totalSlots += availability.getAvailableSlots().size() + availability.getBookedSlots().size();
            bookedSlots += availability.getBookedSlots().size();
            currentDate = currentDate.plusDays(1);
        }

        double occupancyRate = totalSlots > 0 ? (double) bookedSlots / totalSlots * 100 : 0;

        AvailabilityStatsDTO stats = new AvailabilityStatsDTO();
        stats.setBarberId(barberId);
        stats.setStartDate(startDate);
        stats.setEndDate(endDate);
        stats.setTotalDays(totalDays);
        stats.setTotalSlots(totalSlots);
        stats.setBookedSlots(bookedSlots);
        stats.setAvailableSlots(totalSlots - bookedSlots);
        stats.setOccupancyRate(occupancyRate);

        return stats;
    }

     // Classe interne pour les statistiques de disponibilité
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AvailabilityStatsDTO {
        private Long barberId;
        private LocalDate startDate;
        private LocalDate endDate;
        private int totalDays;
        private int totalSlots;
        private int bookedSlots;
        private int availableSlots;
        private double occupancyRate;
    }
    
}
