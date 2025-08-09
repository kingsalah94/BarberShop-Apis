package com.salahtech.BarberShop_Apis.Services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PushNotificationService {

    @Autowired
    private DeviceTokenService deviceTokenService; // Service pour gérer les tokens des appareils

    /**
     * Envoyer une notification push à un utilisateur
     */
    public void sendToUser(Long userId, String title, String body,
                           Map<String, String> data) {
        try {
            String deviceToken = deviceTokenService.getDeviceToken(userId);
            
            if (deviceToken != null && !deviceToken.isEmpty()) {
                sendNotification(deviceToken, title, body, null);
            } else {
                log.warn("No device token found for user: {}", userId);
            }

        } catch (Exception e) {
            log.error("Failed to send push notification to user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Envoyer une notification push avec des données personnalisées
     */
    public void sendToUserWithData(Long userId, String title, String body, Map<String, String> data) {
        try {
            String deviceToken = deviceTokenService.getDeviceToken(userId);
            
            if (deviceToken != null && !deviceToken.isEmpty()) {
                sendNotification(deviceToken, title, body, data);
            } else {
                log.warn("No device token found for user: {}", userId);
            }

        } catch (Exception e) {
            log.error("Failed to send push notification with data to user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Envoyer une notification à un token spécifique
     */
    public void sendNotification(String token, String title, String body, Map<String, String> data) {
        try {
            Notification.Builder notificationBuilder = Notification.builder()
                    .setTitle(title)
                    .setBody(body);

            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(notificationBuilder.build());

            // Ajouter des données personnalisées si fournies
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push notification sent successfully: {}", response);

        } catch (Exception e) {
            log.error("Failed to send push notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Envoyer une notification de réservation
     */
    public void sendBookingNotification(Long userId, String type, String salonName, String dateTime) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "booking");
        data.put("action", type);
        data.put("salon", salonName);
        data.put("datetime", dateTime);

        String title = getBookingNotificationTitle(type);
        String body = getBookingNotificationBody(type, salonName, dateTime);

        sendToUserWithData(userId, title, body, data);
    }

    /**
     * Envoyer une notification de promotion
     */
    public void sendPromotionNotification(Long userId, String promoTitle, String promoDescription) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "promotion");
        data.put("title", promoTitle);

        sendToUserWithData(userId, "Offre spéciale !", promoDescription, data);
    }

    /**
     * Obtenir le titre de notification selon le type de réservation
     */
    private String getBookingNotificationTitle(String type) {
        switch (type.toLowerCase()) {
            case "confirmed":
                return "Réservation confirmée";
            case "cancelled":
                return "Réservation annulée";
            case "reminder":
                return "Rappel de rendez-vous";
            case "completed":
                return "Rendez-vous terminé";
            default:
                return "Notification de réservation";
        }
    }
    

    /**
     * Obtenir le corps de notification selon le type de réservation
     */
    private String getBookingNotificationBody(String type, String salonName, String dateTime) {
        switch (type.toLowerCase()) {
            case "confirmed":
                return String.format("Votre réservation chez %s est confirmée pour le %s", salonName, dateTime);
            case "cancelled":
                return String.format("Votre réservation chez %s du %s a été annulée", salonName, dateTime);
            case "reminder":
                return String.format("N'oubliez pas votre RDV chez %s à %s", salonName, dateTime);
            case "completed":
                return String.format("Merci pour votre visite chez %s. N'hésitez pas à laisser un avis !", salonName);
            default:
                return String.format("Mise à jour concernant votre réservation chez %s", salonName);
        }
    }
}

