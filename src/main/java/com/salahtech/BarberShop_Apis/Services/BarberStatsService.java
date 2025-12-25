package com.salahtech.BarberShop_Apis.Services;

import com.salahtech.BarberShop_Apis.Dtos.BarberStatsDto;
import com.salahtech.BarberShop_Apis.Dtos.BookingTrendDto;
import com.salahtech.BarberShop_Apis.Dtos.PopularServiceDto;
import com.salahtech.BarberShop_Apis.Dtos.UpcomingBookingDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.models.Availability;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.reppsitories.AvailabilityRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BarberRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BarberStatsService {

        private final BookingRepository bookingRepository;
        private final BarberRepository barberRepository;
        private final AvailabilityRepository availabilityRepository;

        public BarberStatsDto getBarberStats(Long barberId) {
                // Vérifier que le barbier existe
                Barber barber = barberRepository.findById(barberId)
                                .orElseThrow(() -> new RuntimeException("Barber not found"));

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
                LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);
                LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate()
                                .atStartOfDay();
                LocalDateTime endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).toLocalDate().atTime(
                                23, 59,
                                59);
                LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay();
                LocalDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).toLocalDate().atTime(23, 59,
                                59);

                return BarberStatsDto.builder()
                                // Statistiques générales
                                .totalBookings(bookingRepository.countByBarberId(barberId))
                                .confirmedBookings(bookingRepository.countByBarberIdAndStatus(barberId,
                                                BookingStatus.CONFIRMED))
                                .pendingBookings(bookingRepository.countByBarberIdAndStatus(barberId,
                                                BookingStatus.PENDING))
                                .cancelledBookings(bookingRepository.countByBarberIdAndStatus(barberId,
                                                BookingStatus.CANCELLED))
                                .completedBookings(bookingRepository.countByBarberIdAndStatus(barberId,
                                                BookingStatus.COMPLETED))

                                // Statistiques financières
                                .totalRevenue(bookingRepository.calculateTotalRevenue(barberId))
                                .dailyRevenue(bookingRepository.calculateRevenueByPeriod(barberId, startOfDay,
                                                endOfDay))
                                .weeklyRevenue(bookingRepository.calculateRevenueByPeriod(barberId, startOfWeek,
                                                endOfWeek))
                                .monthlyRevenue(bookingRepository.calculateRevenueByPeriod(barberId, startOfMonth,
                                                endOfMonth))

                                // Statistiques clients
                                .totalClients(bookingRepository.countUniqueClients(barberId))
                                .newClientsThisMonth(bookingRepository.countNewClientsThisMonth(barberId, startOfMonth))
                                .returningClients(bookingRepository.countReturningClients(barberId))

                                // Statistiques de performance
                                .averageRating(bookingRepository.calculateAverageRating(barberId))
                                .totalReviews(bookingRepository.countReviews(barberId))

                                // Prochains rendez-vous
                                .upcomingBookings(getUpcomingBookings(barberId, now))

                                // Tendances
                                .weeklyTrend(getWeeklyTrend(barberId, startOfWeek, endOfWeek))
                                .monthlyTrend(getMonthlyTrend(barberId, startOfMonth))

                                // Services populaires
                                .popularServices(getPopularServices(barberId))

                                // Disponibilité et taux d'occupation
                                .availableHoursThisWeek(calculateAvailableHours(barberId, startOfWeek, endOfWeek))
                                .bookedHoursThisWeek(calculateBookedHours(barberId, startOfWeek, endOfWeek))
                                .occupancyRate(calculateOccupancyRate(barberId, startOfWeek, endOfWeek))

                                .build();
        }

        private List<UpcomingBookingDto> getUpcomingBookings(Long barberId, LocalDateTime now) {
                List<Booking> upcomingBookings = bookingRepository.findUpcomingBookings(barberId, now);

                return upcomingBookings.stream()
                                .limit(5) // Limiter à 5 prochains rendez-vous
                                .map(booking -> UpcomingBookingDto.builder()
                                                .bookingId(booking.getId())
                                                .clientName(booking.getClient().getUser().getFirstName() + " " +
                                                                booking.getClient().getUser().getLastName())
                                                .serviceName(booking.getBarberService().getName())
                                                .appointmentDate(booking.getStartAt())
                                                .status(booking.getStatus().name())
                                                .price(booking.getTotalPrice())
                                                .build())
                                .collect(Collectors.toList());
        }

        private List<BookingTrendDto> getWeeklyTrend(Long barberId,
                        LocalDateTime startOfWeek,
                        LocalDateTime endOfWeek) {
                List<Object[]> trends = bookingRepository.findBookingTrend(barberId, startOfWeek, endOfWeek);

                return trends.stream()
                                .map(row -> BookingTrendDto.builder()
                                                .period(row[0].toString()) // Date
                                                .bookingCount(((Number) row[1]).longValue())
                                                .revenue((BigDecimal) row[2])
                                                .build())
                                .collect(Collectors.toList());
        }

        private List<BookingTrendDto> getMonthlyTrend(Long barberId, LocalDateTime startOfMonth) {
                List<BookingTrendDto> monthlyTrend = new ArrayList<>();

                LocalDateTime current = startOfMonth;
                LocalDateTime now = LocalDateTime.now();

                for (int i = 0; i < 12; i++) {
                        LocalDateTime monthStart = current.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate()
                                        .atStartOfDay();
                        LocalDateTime monthEnd = current.with(TemporalAdjusters.lastDayOfMonth()).toLocalDate()
                                        .atTime(23, 59, 59);

                        if (monthStart.isAfter(now))
                                break;

                        Long count = bookingRepository.countBookingsByPeriod(barberId, monthStart, monthEnd);
                        BigDecimal revenue = bookingRepository.calculateRevenueByPeriod(barberId, monthStart, monthEnd);

                        monthlyTrend.add(BookingTrendDto.builder()
                                        .period(monthStart.toLocalDate().toString())
                                        .bookingCount(count)
                                        .revenue(revenue)
                                        .build());

                        current = current.minusMonths(1);
                }

                return monthlyTrend;
        }

        private List<PopularServiceDto> getPopularServices(Long barberId) {
                List<Object[]> services = bookingRepository.findPopularServices(barberId);

                return services.stream()
                                .limit(5) // Top 5 services
                                .map(row -> PopularServiceDto.builder()
                                                .serviceId(((Number) row[0]).longValue())
                                                .serviceName((String) row[1])
                                                .bookingCount(((Number) row[2]).longValue())
                                                .totalRevenue((BigDecimal) row[3])
                                                .averageRating(row[4] != null ? ((Number) row[4]).doubleValue() : null)
                                                .build())
                                .collect(Collectors.toList());
        }

        /**
         * Calcule les heures disponibles basées sur les Availability récurrentes
         * (DayOfWeek)
         */
        private Integer calculateAvailableHours(Long barberId, LocalDateTime startOfWeek, LocalDateTime endOfWeek) {
                int totalMinutes = 0;
                LocalDate current = startOfWeek.toLocalDate();
                LocalDate end = endOfWeek.toLocalDate();

                while (!current.isAfter(end)) {
                        DayOfWeek dayOfWeek = current.getDayOfWeek();

                        // Récupérer les disponibilités récurrentes pour ce jour
                        List<Availability> dayAvailabilities = availabilityRepository
                                        .findByBarberIdAndDayOfWeek(barberId, dayOfWeek)
                                        .stream()
                                        .filter(a -> Boolean.TRUE.equals(a.getIsAvailable()))
                                        .collect(Collectors.toList());

                        // Vérifier s'il y a des exceptions pour ce jour spécifique
                        List<Availability> exceptions = availabilityRepository
                                        .findByBarberIdAndDate(barberId, current)
                                        .stream()
                                        .filter(a -> a.getExceptionType() != null)
                                        .collect(Collectors.toList());

                        // Si exception CLOSED, ce jour n'est pas disponible
                        boolean isClosed = exceptions.stream()
                                        .anyMatch(e -> "CLOSED".equalsIgnoreCase(e.getExceptionType()));

                        if (isClosed) {
                                current = current.plusDays(1);
                                continue;
                        }

                        // Si exception CUSTOM, utiliser ces horaires au lieu des récurrents
                        List<Availability> customExceptions = exceptions.stream()
                                        .filter(e -> "CUSTOM".equalsIgnoreCase(e.getExceptionType()))
                                        .collect(Collectors.toList());

                        List<Availability> effectiveAvailabilities = customExceptions.isEmpty()
                                        ? dayAvailabilities
                                        : customExceptions;

                        // Calculer les minutes pour ce jour
                        for (Availability availability : effectiveAvailabilities) {
                                if (availability.getStartAt() != null && availability.getEndAt() != null) {
                                        long minutes = java.time.Duration.between(
                                                        availability.getStartAt(),
                                                        availability.getEndAt()).toMinutes();
                                        totalMinutes += minutes;
                                }
                        }

                        current = current.plusDays(1);
                }

                return totalMinutes / 60;
        }

        private Integer calculateBookedHours(Long barberId, LocalDateTime startOfWeek, LocalDateTime endOfWeek) {
                List<Booking> bookings = bookingRepository.findByBarber_Id(barberId).stream()
                                .filter(b -> !b.getStartAt().isBefore(startOfWeek) &&
                                                !b.getStartAt().isAfter(endOfWeek) &&
                                                b.getStatus() != BookingStatus.CANCELLED)
                                .collect(Collectors.toList());

                int totalMinutes = bookings.stream()
                                .mapToInt(b -> b.getBarberService().getDuration())
                                .sum();

                return totalMinutes / 60;
        }

        private Double calculateOccupancyRate(Long barberId, LocalDateTime startOfWeek, LocalDateTime endOfWeek) {
                Integer availableHours = calculateAvailableHours(barberId, startOfWeek, endOfWeek);
                Integer bookedHours = calculateBookedHours(barberId, startOfWeek, endOfWeek);

                if (availableHours == 0)
                        return 0.0;

                double rate = (bookedHours.doubleValue() / availableHours.doubleValue()) * 100;
                return BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
}