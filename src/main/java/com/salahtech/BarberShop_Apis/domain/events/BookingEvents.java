package com.salahtech.BarberShop_Apis.domain.events;

public class BookingEvents {

    public record BookingCreatedEvent(Long bookingId) {}
    public record BookingConfirmedEvent(Long bookingId) {}
    public record BookingCancelledEvent(Long bookingId) {}
    public record BookingReminderDueEvent(Long bookingId) {}

        // Additional events can be added here as needed
    // This class can be used to group all booking-related events
}
