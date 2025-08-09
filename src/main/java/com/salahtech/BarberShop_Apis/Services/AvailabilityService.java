package com.salahtech.BarberShop_Apis.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.salahtech.BarberShop_Apis.Dtos.AvailabilityDto;
import com.salahtech.BarberShop_Apis.Dtos.TimeSlotDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.reppsitories.BarberRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@Slf4j
public class AvailabilityService {

    @Autowired
    private BarberRepository barberRepository;

    @Autowired
    private BookingRepository bookingRepository;

    // Horaires de travail par défaut
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(18, 0);
    private static final int DEFAULT_SLOT_DURATION = 30; // minutes

    /**
     * Obtenir la disponibilité d'un barbier pour une date donnée
     */
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
    public boolean isTimeSlotAvailable(Long barberId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
            barberId, startTime, endTime);
        
        return conflictingBookings.isEmpty();
    }

    /**
     * Obtenir les prochains créneaux disponibles pour un barbier
     */
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
    public List<AvailabilityDto> getAllBarbersAvailability(LocalDate date) {
        List<Barber> activeBarbers = barberRepository.findByAvailable(true);
        
        return activeBarbers.stream()
            .map(barber -> getBarberAvailability(barber.getId(), date))
            .collect(Collectors.toList());
    }

    /**
     * Générer les créneaux horaires pour une date donnée
     */
    private List<TimeSlotDto> generateTimeSlots(LocalDate date, String workingHours) {
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
