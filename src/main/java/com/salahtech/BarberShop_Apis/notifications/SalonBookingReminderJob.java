package com.salahtech.BarberShop_Apis.notifications;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonBookingService;

import lombok.RequiredArgsConstructor;

/** Rappel la veille (09:00, heure de Niamey) des réservations salon confirmées. */
@Component
@RequiredArgsConstructor
public class SalonBookingReminderJob {

    private static final ZoneId ZONE = ZoneId.of("Africa/Niamey");

    private final SalonBookingService salonBookingService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Africa/Niamey")
    public void remindTomorrow() {
        salonBookingService.sendRemindersFor(LocalDate.now(ZONE).plusDays(1));
    }
}
