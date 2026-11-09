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

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

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


    // Faites de même pour getRevenueTrend et getStatusBreakdown en ajoutant le "if
    // (barberId != null)"

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

}
