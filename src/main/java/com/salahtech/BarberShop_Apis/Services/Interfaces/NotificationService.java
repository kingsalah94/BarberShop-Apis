package com.salahtech.BarberShop_Apis.Services.Interfaces;


import com.salahtech.BarberShop_Apis.Dtos.NotificationDto;
import com.salahtech.BarberShop_Apis.models.Booking;

import java.util.concurrent.CompletableFuture;

public interface NotificationService {

    /**
     * Envoie une confirmation de réservation au client et au barbier.
     * @param booking La réservation concernée.
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> sendBookingConfirmation(Booking booking);

    /**
     * Envoie un rappel au client avant son rendez-vous.
     * @param booking La réservation concernée.
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> sendBookingReminder(Booking booking);

    /**
     * Envoie une notification d'annulation au client et au barbier.
     * @param booking La réservation annulée.
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> sendBookingCancellation(Booking booking);

    /**
     * Envoie une notification personnalisée.
     * @param notification Les détails de la notification.
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> sendCustomNotification(NotificationDto notification);
}

