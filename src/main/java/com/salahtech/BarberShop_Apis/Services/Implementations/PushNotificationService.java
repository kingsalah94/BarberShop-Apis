package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.DeviceTokenService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final ApplicationUserRepository userRepository;
    private final DeviceTokenService deviceTokenService;
    private final FirebaseMessaging firebaseMessaging; // Injection du Bean configuré

    // ========================================================================
    // 🛠️ MÉTHODES PUBLIQUES DE HAUT NIVEAU
    // ========================================================================

    /**
     * Envoyer une notification générique à un utilisateur (avec ou sans data)
     */
    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
        String deviceToken = deviceTokenService.getDeviceToken(userId);

        if (deviceToken != null && !deviceToken.isEmpty()) {
            executeSend(deviceToken, title, body, data);
        } else {
            log.warn("Tentative d'envoi échouée : Aucun token trouvé pour l'utilisateur ID {}", userId);
        }
    }

    /**
     * Surcharge pour envoyer sans data
     */
    public void sendToUser(Long userId, String title, String body) {
        sendToUser(userId, title, body, null);
    }

    /**
     * Envoyer une notification liée à une Réservation
     */
    public void sendBookingNotification(Long userId, String type, String salonName, String dateTime) {
        NotificationTemplate template = NotificationTemplate.fromString(type);

        String title = template.getTitle();
        String body = String.format(template.getBodyFormat(), salonName, dateTime);

        Map<String, String> data = new HashMap<>();
        data.put("type", "booking");
        data.put("action", type);
        data.put("salon", salonName);
        data.put("datetime", dateTime);
        data.put("click_action", "FLUTTER_NOTIFICATION_CLICK"); // Important pour le mobile

        sendToUser(userId, title, body, data);
    }

    /**
     * Envoyer une Promotion
     */
    public void sendPromotionNotification(Long userId, String promoTitle, String promoDescription) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "promotion");
        data.put("click_action", "FLUTTER_NOTIFICATION_CLICK");

        sendToUser(userId, "Offre Spéciale ! 🎉", promoDescription, data);
    }

    /**
     * Met à jour le token FCM en base (appelé au login mobile)
     */
    @Transactional
    public void updateFcmToken(String email, String newToken) {
        ApplicationUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable pour update token"));

        user.setFcmToken(newToken);
        userRepository.save(user);
        log.info("Token FCM mis à jour pour {}", email);
    }

    // ========================================================================
    // ⚙️ COEUR DU SYSTÈME (MÉTHODE PRIVÉE)
    // ========================================================================

    /**
     * Méthode unique qui communique avec Firebase.
     * Toutes les autres méthodes passent par ici.
     */
    private void executeSend(String token, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.error("Firebase n'est pas initialisé. Impossible d'envoyer la notification.");
            return;
        }

        try {
            // 1. Construire la notif visuelle
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            // 2. Construire le message
            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(notification);

            // 3. Ajouter les data si elles existent
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            // 4. Envoyer
            String response = firebaseMessaging.send(messageBuilder.build());
            log.info("Notification envoyée avec succès (Token: {}...) : ID {}",
                    token.substring(0, Math.min(token.length(), 10)), response);

        } catch (Exception e) {
            log.error("Erreur critique lors de l'envoi FCM : {}", e.getMessage());
            // Optionnel : Vous pourriez vouloir supprimer le token s'il est invalide
            // (NotRegistered)
        }
    }

    // ========================================================================
    // 📝 GESTION DES TEXTES (ENUM)
    // ========================================================================

    @Getter
    @RequiredArgsConstructor
    private enum NotificationTemplate {
        CONFIRMED("Réservation confirmée ✅", "Votre réservation chez %s est confirmée pour le %s"),
        CANCELLED("Réservation annulée ❌", "Votre réservation chez %s du %s a été annulée"),
        REMINDER("Rappel de rendez-vous ⏰", "N'oubliez pas votre RDV chez %s à %s"),
        COMPLETED("Rendez-vous terminé ⭐", "Merci pour votre visite chez %s. N'hésitez pas à laisser un avis !"),
        DEFAULT("Mise à jour réservation 📅", "Mise à jour concernant votre réservation chez %s le %s");

        private final String title;
        private final String bodyFormat;

        public static NotificationTemplate fromString(String type) {
            try {
                return valueOf(type.toUpperCase());
            } catch (Exception e) {
                return DEFAULT;
            }
        }
    }
}