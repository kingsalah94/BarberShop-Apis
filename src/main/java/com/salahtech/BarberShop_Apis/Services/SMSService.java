package com.salahtech.BarberShop_Apis.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SMSService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;

    private boolean initialized = false;

    /**
     * Initialiser Twilio
     */
    private void initializeTwilio() {
        if (!initialized) {
            Twilio.init(accountSid, authToken);
            initialized = true;
            log.info("Twilio initialized successfully");
        }
    }

    /**
     * Envoyer un SMS
     */
    public void sendSMS(String to, String messageText) {
        try {
            initializeTwilio();

            // Valider le numéro de téléphone
            if (to == null || to.trim().isEmpty()) {
                log.warn("Cannot send SMS: phone number is empty");
                return;
            }

            // S'assurer que le numéro commence par +
            if (!to.startsWith("+")) {
                to = "+33" + to.substring(1); // Exemple pour la France
            }

            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(twilioPhoneNumber),
                    messageText
            ).create();

            log.info("SMS sent successfully to {} with SID: {}", to, message.getSid());

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", to, e.getMessage(), e);
        }
    }

    /**
     * Envoyer un SMS de confirmation de réservation
     */
    public void sendBookingConfirmationSMS(String to, String salonName, String date, String time) {
        String message = String.format(
            "Réservation confirmée chez %s le %s à %s. À bientôt !",
            salonName, date, time
        );
        sendSMS(to, message);
    }

    /**
     * Envoyer un SMS de rappel
     */
    public void sendBookingReminderSMS(String to, String salonName, String time, int hoursBeforeType) {
        String timeText = hoursBeforeType == 24 ? "demain" : "dans 2h";
        String message = String.format(
            "Rappel: Votre RDV chez %s %s à %s. À bientôt !",
            salonName, timeText, time
        );
        sendSMS(to, message);
    }

    /**
     * Envoyer un SMS d'annulation
     */
    public void sendBookingCancellationSMS(String to, String salonName, String dateTime) {
        String message = String.format(
            "Votre réservation chez %s du %s a été annulée.",
            salonName, dateTime
        );
        sendSMS(to, message);
    }

    /**
     * Envoyer un code de vérification par SMS
     */
    public void sendVerificationCode(String to, String code) {
        String message = String.format(
            "Votre code de vérification est: %s. Ne le partagez jamais.",
            code
        );
        sendSMS(to, message);
    }
}
