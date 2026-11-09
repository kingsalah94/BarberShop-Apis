package com.salahtech.BarberShop_Apis.Services.Interfaces;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.salahtech.BarberShop_Apis.Dtos.DashboardStatsDto;
import com.salahtech.BarberShop_Apis.Dtos.TimePointDecimalDto;
import com.salahtech.BarberShop_Apis.Dtos.TimePointLongDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;

public interface DashboardService {

    DashboardStatsDto getAllStatsForAdmin();


    List<TimePointLongDto> getBookingsTrend(LocalDate from, LocalDate to);

    List<TimePointDecimalDto> getAllRevenueTrend(LocalDate from, LocalDate to);

    Map<BookingStatus, Long> getAllStatusBreakdown(LocalDate from, LocalDate to);
}
