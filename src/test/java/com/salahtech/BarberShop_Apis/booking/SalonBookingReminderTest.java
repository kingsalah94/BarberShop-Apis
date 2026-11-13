package com.salahtech.BarberShop_Apis.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Services.Implementations.SalonBookingServiceImpl;
import com.salahtech.BarberShop_Apis.notifications.UserNotifier;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Client;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.models.SalonBooking;
import com.salahtech.BarberShop_Apis.models.SalonOffering;
import com.salahtech.BarberShop_Apis.repositories.SalonBookingRepository;

/** Rappel la veille des réservations salon confirmées. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalonBookingReminderTest {

    @Mock SalonBookingRepository bookingRepository;
    @Mock UserNotifier userNotifier;
    @InjectMocks SalonBookingServiceImpl service;

    private final LocalDate tomorrow = LocalDate.now().plusDays(1);

    private SalonBooking booking(long id, long userId) {
        ApplicationUser u = new ApplicationUser();
        u.setId(userId);
        Client c = new Client();
        c.setUser(u);
        Salon s = new Salon();
        s.setName("Oberkampf");
        SalonBooking b = new SalonBooking();
        b.setId(id);
        b.setClient(c);
        b.setSalon(s);
        b.setOffering(SalonOffering.builder().name("Coupe").build());
        b.setStatus(BookingStatus.CONFIRMED);
        b.setStartAt(tomorrow.atTime(10, 30));
        b.setEndAt(tomorrow.atTime(11, 0));
        return b;
    }

    @Test
    void chaqueClientAvecCompteRecoitLeRappelUneSeuleFois() {
        when(bookingRepository.findByStatusInAndStartAtBetween(any(), any(), any()))
                .thenReturn(List.of(booking(1, 100), booking(2, 200)));

        int sent = service.sendRemindersFor(tomorrow);

        assertThat(sent).isEqualTo(2);
        verify(userNotifier).notifyOnce(eq(100L), eq("BOOKING_REMINDER"), contains("Rappel"), contains("10:30"), eq(1L));
        verify(userNotifier).notifyOnce(eq(200L), eq("BOOKING_REMINDER"), anyString(), anyString(), eq(2L));
    }

    @Test
    void unEchecDEnvoiNeBloquePasLesAutres() {
        when(bookingRepository.findByStatusInAndStartAtBetween(any(), any(), any()))
                .thenReturn(List.of(booking(1, 100), booking(2, 200)));
        doThrow(new RuntimeException("down")).when(userNotifier)
                .notifyOnce(eq(100L), anyString(), anyString(), anyString(), any());

        assertThat(service.sendRemindersFor(tomorrow)).isEqualTo(1);
        verify(userNotifier).notifyOnce(eq(200L), anyString(), anyString(), anyString(), eq(2L));
    }

    @Test
    void sansCompteClientPasDeRappel() {
        SalonBooking anon = booking(3, 300);
        anon.getClient().setUser(null);
        when(bookingRepository.findByStatusInAndStartAtBetween(any(), any(), any())).thenReturn(List.of(anon));

        assertThat(service.sendRemindersFor(tomorrow)).isZero();
        verify(userNotifier, never()).notifyOnce(any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void seulementLesStatutsConfirmes() {
        service.sendRemindersFor(tomorrow);

        verify(bookingRepository).findByStatusInAndStartAtBetween(
                eq(List.of(BookingStatus.CONFIRMED, BookingStatus.BOOKED)),
                eq(tomorrow.atStartOfDay()), eq(tomorrow.plusDays(1).atStartOfDay()));
    }
}
