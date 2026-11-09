package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.NotificationDto;
import com.salahtech.BarberShop_Apis.Dtos.NotificationMessageDto;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface NotificationService {

    /**
     * Envoie une notification personnalisée.
     * 
     * @param notification Les détails de la notification.
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> sendCustomNotification(NotificationDto notification);

    void sendNotification(String targetToken, String title, String body, Map<String, String> data);

    void sendBookingNotification(String targetToken, String title, String body, Long bookingId);

    List<NotificationMessageDto> getUserNotifications(Long userId);

}
