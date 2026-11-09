// src/main/java/.../Services/Implementations/DashboardServiceImpl.java
package com.salahtech.BarberShop_Apis.Services.Implementations;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.salahtech.BarberShop_Apis.Dtos.*;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Services.Interfaces.DashboardService;
import com.salahtech.BarberShop_Apis.repositories.BookingRepository;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;
    /** Vues plateforme (ADMIN) : réservations du modèle salon. */
    private final com.salahtech.BarberShop_Apis.repositories.SalonBookingRepository salonBookings;
    private static final ZoneId ZONE = ZoneId.of("Africa/Niamey");

    @Override
    public DashboardStatsDto getAllStatsForAdmin() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime now = LocalDateTime.now(ZONE);

        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime endToday = today.plusDays(1).atStartOfDay();

        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDateTime startMonth = firstOfMonth.atStartOfDay();

        LocalDate startOfWeekDate = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime startWeek = startOfWeekDate.atStartOfDay();

        long total = salonBookings.count();
        long completed = salonBookings.countByStatus(BookingStatus.COMPLETED);
        long pending = salonBookings.countByStatus(BookingStatus.PENDING);
        long cancelled = salonBookings.countByStatus(BookingStatus.CANCELLED);

        BigDecimal totalRevenue = nvl(salonBookings.sumCompletedRevenue());
        BigDecimal monthlyRevenue = nvl(salonBookings.sumCompletedRevenueBetween(startMonth, now));
        BigDecimal dailyRevenue = nvl(salonBookings.sumCompletedRevenueBetween(startToday, endToday));

        long todayBookings = salonBookings.countByStartAtGreaterThanEqualAndStartAtLessThan(startToday, endToday);
        long weekBookings = salonBookings.countByStartAtGreaterThanEqualAndStartAtLessThan(startWeek, now);
        long monthBookings = salonBookings.countByStartAtGreaterThanEqualAndStartAtLessThan(startMonth, now);

        return new DashboardStatsDto(
                total, completed, pending, cancelled,
                totalRevenue, monthlyRevenue, dailyRevenue,
                todayBookings, weekBookings, monthBookings,
                now);
    }

    // ====== Nouveaux services pour graphiques ======
    public List<TimePointLongDto> getBookingsTrend(LocalDate from, LocalDate to) {
        List<TimePointLongDto> out = new ArrayList<>();
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            LocalDateTime start = cur.atStartOfDay();
            LocalDateTime end = cur.plusDays(1).atStartOfDay();
            long cnt = salonBookings.countByStartAtGreaterThanEqualAndStartAtLessThan(start, end);
            out.add(new TimePointLongDto(cur, cnt));
            cur = cur.plusDays(1);
        }
        return out;
    }

    @Override
    public List<TimePointDecimalDto> getAllRevenueTrend(LocalDate from, LocalDate to) {
        List<TimePointDecimalDto> out = new ArrayList<>();
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            LocalDateTime start = cur.atStartOfDay();
            LocalDateTime end = cur.plusDays(1).atStartOfDay();
            BigDecimal val = nvl(salonBookings.sumCompletedRevenueBetween(start, end));
            out.add(new TimePointDecimalDto(cur, val));
            cur = cur.plusDays(1);
        }
        return out;
    }

    @Override
    public Map<BookingStatus, Long> getAllStatusBreakdown(LocalDate from, LocalDate to) {
        Map<BookingStatus, Long> map = new EnumMap<>(BookingStatus.class);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        for (BookingStatus st : BookingStatus.values()) {
            long c = salonBookings.countByStatusAndStartAtGreaterThanEqualAndStartAtLessThan(st, start, end);
            map.put(st, c);
        }
        return map;
    }

    // private static BigDecimal nvl(BigDecimal v) {
    // return v == null ? BigDecimal.ZERO : v;
    // }

    @Override
    public DashboardStatsDto getStatsByBarber(Long barberId) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime endToday = today.plusDays(1).atStartOfDay();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDateTime startMonth = firstOfMonth.atStartOfDay();
        LocalDate startOfWeekDate = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime startWeek = startOfWeekDate.atStartOfDay();

        // LOGIQUE HYBRIDE (BARBIER vs ADMIN)
        long total, completed, pending, cancelled, todayBk, weekBk, monthBk;
        BigDecimal totalRev, monthlyRev, dailyRev;

        if (barberId != null) {
            // --- CAS BARBIER (Filtré) ---
            total = bookingRepository.countByBarberId(barberId);
            completed = bookingRepository.countByBarberIdAndStatus(barberId, BookingStatus.COMPLETED);
            pending = bookingRepository.countByBarberIdAndStatus(barberId, BookingStatus.PENDING);
            cancelled = bookingRepository.countByBarberIdAndStatus(barberId, BookingStatus.CANCELLED);

            totalRev = nvl(bookingRepository.sumRevenueByBarber(barberId));
            monthlyRev = nvl(bookingRepository.sumRevenueByBarberBetween(barberId, startMonth, now));
            dailyRev = nvl(bookingRepository.sumRevenueByBarberBetween(barberId, startToday, endToday));

            todayBk = bookingRepository.countByBarberBetween(barberId, startToday, endToday);
            weekBk = bookingRepository.countByBarberBetween(barberId, startWeek, now);
            monthBk = bookingRepository.countByBarberBetween(barberId, startMonth, now);
        } else {
            // --- CAS ADMIN (Global - Vos anciennes méthodes) ---
            total = bookingRepository.count();
            completed = bookingRepository.countByStatus(BookingStatus.COMPLETED);
            // ... etc (je raccourcis pour la lisibilité)
            // Utilisez vos méthodes existantes non filtrées ici
            totalRev = BigDecimal.ZERO; // Placeholder
            monthlyRev = BigDecimal.ZERO;
            dailyRev = BigDecimal.ZERO;
            pending = 0;
            cancelled = 0;
            todayBk = 0;
            weekBk = 0;
            monthBk = 0;
        }

        return DashboardStatsDto.builder()
                .totalBookings(total)
                .completedBookings(completed)
                .pendingBookings(pending)
                .cancelledBookings(cancelled)
                .totalRevenue(totalRev)
                .monthlyRevenue(monthlyRev)
                .dailyRevenue(dailyRev)
                .todayBookings(todayBk)
                .weekBookings(weekBk)
                .monthBookings(monthBk)
                .lastUpdated(now)
                .build();
    }

    @Override
    public List<TimePointLongDto> getBookingsTrend(Long barberId, LocalDate from, LocalDate to) {
        List<TimePointLongDto> out = new ArrayList<>();
        LocalDate cur = from;

        // Note: Pour optimiser ça (éviter la boucle), il faudrait une requête GROUP BY
        // en SQL
        // Mais gardons la boucle pour l'instant, c'est plus simple à lire.
        while (!cur.isAfter(to)) {
            LocalDateTime start = cur.atStartOfDay();
            LocalDateTime end = cur.plusDays(1).atStartOfDay();
            long cnt;

            if (barberId != null) {
                cnt = bookingRepository.countByBarberBetween(barberId, start, end);
            } else {
                cnt = bookingRepository.countBetween(start, end);
            }

            out.add(new TimePointLongDto(cur, cnt));
            cur = cur.plusDays(1);
        }
        return out;
    }

    // Faites de même pour getRevenueTrend et getStatusBreakdown en ajoutant le "if
    // (barberId != null)"

    @Override
    public Map<BookingStatus, Long> getStatusBreakdown(Long barberId, LocalDate from, LocalDate to) {
        Map<BookingStatus, Long> map = new EnumMap<>(BookingStatus.class);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        for (BookingStatus st : BookingStatus.values()) {
            long c;
            if (barberId != null) {
                c = bookingRepository.countByBarberAndStatusBetween(barberId, st, start, end);
            } else {
                c = bookingRepository.countByStatusBetween(st, start, end);
            }
            map.put(st, c);
        }
        return map;
    }

    @Override
    public List<TimePointDecimalDto> getRevenueTrend(Long barberId, LocalDate from, LocalDate to) {
        List<TimePointDecimalDto> out = new ArrayList<>();
        LocalDate cur = from;

        // On boucle sur chaque jour de la période demandée
        while (!cur.isAfter(to)) {
            LocalDateTime start = cur.atStartOfDay();
            LocalDateTime end = cur.plusDays(1).atStartOfDay();

            BigDecimal dailyTotal;

            if (barberId != null) {
                // Cas BARBIER : On utilise la méthode filtrée par ID
                dailyTotal = bookingRepository.sumRevenueByBarberBetween(barberId, start, end);
            } else {
                // Cas ADMIN : On utilise la méthode globale (somme de tous les barbiers)
                dailyTotal = bookingRepository.sumGlobalRevenueBetween(start, end);
            }

            // Important : Si aucun RDV, SQL renvoie NULL. On convertit en ZÉRO via nvl()
            out.add(new TimePointDecimalDto(cur, nvl(dailyTotal)));

            cur = cur.plusDays(1);
        }
        return out;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

}
