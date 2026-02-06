package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.NotificationDto;
import com.salahtech.BarberShop_Apis.Dtos.NotificationMessageDto;
import com.salahtech.BarberShop_Apis.models.Booking;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface NotificationService {

    /**
     * Envoie une confirmation de réservation au client et au barbier.
     * 
     * @param booking La réservation concernée.
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> sendBookingConfirmation(Booking booking);

    /**
     * Envoie un rappel au client avant son rendez-vous.
     * 
     * @param booking La réservation concernée.
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> sendBookingReminder(Booking booking);

    /**
     * Envoie une notification d'annulation au client et au barbier.
     * 
     * @param booking La réservation annulée.
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> sendBookingCancellation(Booking booking);

    /**
     * Envoie une notification personnalisée.
     * 
     * @param notification Les détails de la notification.
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> sendCustomNotification(NotificationDto notification);

    void sendNotification(String targetToken, String title, String body, Map<String, String> data);

    void sendBookingNotification(String targetToken, String title, String body, Long bookingId);

    void resendBookingConfirmation(Long bookingId);

    void resendBookingReminder(Long bookingId);

    void resendBookingCancellation(Long bookingId);

    List<NotificationMessageDto> getUserNotifications(Long userId);

    void generateNotificationsFromExistingBookings(Long barberId);
}
