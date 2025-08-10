package com.salahtech.BarberShop_Apis.notifications;


import com.salahtech.BarberShop_Apis.Services.Interfaces.NotificationService;
import com.salahtech.BarberShop_Apis.domain.events.*;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingNotificationListener {

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCreated(BookingCreatedEvent event) {
        sendConfirmation(event.bookingId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        sendConfirmation(event.bookingId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelledEvent event) {
        var booking = load(event.bookingId());
        notificationService.sendBookingCancellation(booking);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReminderDue(BookingReminderDueEvent event) {
        var booking = load(event.bookingId());
        notificationService.sendBookingReminder(booking);
    }

    private void sendConfirmation(Long bookingId) {
        var booking = load(bookingId);
        notificationService.sendBookingConfirmation(booking);
    }

    private Booking load(Long id) {
        return bookingRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("Booking introuvable: " + id));
    }
}
