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
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;
    private static final ZoneId ZONE = ZoneId.of("Africa/Niamey");

    @Override
    public DashboardStatsDto getStats() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime now = LocalDateTime.now(ZONE);

        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime endToday = today.plusDays(1).atStartOfDay();

        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDateTime startMonth = firstOfMonth.atStartOfDay();

        LocalDate startOfWeekDate = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime startWeek = startOfWeekDate.atStartOfDay();

        long total = bookingRepository.count();
        long completed = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        long pending = bookingRepository.countByStatus(BookingStatus.PENDING);
        long cancelled = bookingRepository.countByStatus(BookingStatus.CANCELLED);

        BigDecimal totalRevenue = nvl(bookingRepository.sumRevenueAllCompleted());
        BigDecimal monthlyRevenue = nvl(bookingRepository.sumRevenueCompletedBetween(startMonth, now));
        BigDecimal dailyRevenue = nvl(bookingRepository.sumRevenueCompletedBetween(startToday, endToday));

        long todayBookings = bookingRepository.countBetween(startToday, endToday);
        long weekBookings = bookingRepository.countBetween(startWeek, now);
        long monthBookings = bookingRepository.countBetween(startMonth, now);

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
            long cnt = bookingRepository.countBetween(start, end);
            out.add(new TimePointLongDto(cur, cnt));
            cur = cur.plusDays(1);
        }
        return out;
    }

    public List<TimePointDecimalDto> getRevenueTrend(LocalDate from, LocalDate to) {
        List<TimePointDecimalDto> out = new ArrayList<>();
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            LocalDateTime start = cur.atStartOfDay();
            LocalDateTime end = cur.plusDays(1).atStartOfDay();
            BigDecimal val = nvl(bookingRepository.sumRevenueCompletedBetween(start, end));
            out.add(new TimePointDecimalDto(cur, val));
            cur = cur.plusDays(1);
        }
        return out;
    }

    public Map<BookingStatus, Long> getStatusBreakdown(LocalDate from, LocalDate to) {
        Map<BookingStatus, Long> map = new EnumMap<>(BookingStatus.class);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        for (BookingStatus st : BookingStatus.values()) {
            long c = bookingRepository.countByStatusBetween(st, start, end);
            map.put(st, c);
        }
        return map;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
