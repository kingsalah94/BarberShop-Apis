// src/main/java/.../Web/Controller/DashboardController.java
package com.salahtech.BarberShop_Apis.Web.Controller;

import java.time.LocalDate;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import com.salahtech.BarberShop_Apis.Dtos.*;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Services.Implementations.DashboardServiceImpl;
import com.salahtech.BarberShop_Apis.Services.Interfaces.DashboardService;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardController {

    private final DashboardService service;
    private final DashboardServiceImpl impl; // pour les méthodes “extra”

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> stats() {
        return ResponseEntity.ok(service.getStats());
    }

    @GetMapping("/trends/bookings")
    public ResponseEntity<List<TimePointLongDto>> bookingsTrend(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return ResponseEntity.ok(impl.getBookingsTrend(from, to));
    }

    @GetMapping("/trends/revenue")
    public ResponseEntity<List<TimePointDecimalDto>> revenueTrend(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return ResponseEntity.ok(impl.getRevenueTrend(from, to));
    }

    @GetMapping("/breakdown/status")
    public ResponseEntity<Map<BookingStatus, Long>> statusBreakdown(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return ResponseEntity.ok(impl.getStatusBreakdown(from, to));
    }
}
