// ===========================================
// 4. NOTIFICATION SERVICE OPTIMISÉ
// ===========================================

package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.NotificationDto;
import com.salahtech.BarberShop_Apis.Enums.NotificationType;
import com.salahtech.BarberShop_Apis.Services.EmailService;
import com.salahtech.BarberShop_Apis.Services.PushNotificationService;
import com.salahtech.BarberShop_Apis.Services.SMSService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.NotificationService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    
    private final EmailService emailService;
    private final SMSService smsService;
    private final PushNotificationService pushNotificationService;
    
    @Override
    @Async
    public CompletableFuture<Void> sendBookingConfirmation(Booking booking) {
        log.info("Envoi de confirmation de réservation pour la réservation {}", booking.getId());
        
        Map<String, Object> templateVariables = createBookingTemplateVariables(booking);
        
        try {
            // Email de confirmation
            if (booking.getUser().getEmail() != null) {
                emailService.sendTemplateEmail(
                    booking.getUser().getEmail(),
                    "Confirmation de votre réservation",
                    "booking-confirmation",
                    templateVariables
                );
            }
            
            // SMS de confirmation
            if (booking.getUser().getPhone() != null) {
                String smsMessage = String.format(
                    "Réservation confirmée chez %s le %s à %s. Réf: #%d",
                    booking.getBarber().getSalonName(),
                    booking.getBookingDate().toLocalDate(),
                    //booking.getBookingTime().toLocalTime(),
                    booking.getId()
                );
                smsService.sendSMS(booking.getUser().getPhone(), smsMessage);
            }
            
            // Push notification
            pushNotificationService.sendToUser(
                booking.getUser().getId(),
                "Réservation confirmée",
                "Votre réservation a été confirmée avec succès", null
            );
            
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications de confirmation", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    @Async
    public CompletableFuture<Void> sendBookingReminder(Booking booking) {
        log.info("Envoi de rappel de réservation pour la réservation {}", booking.getId());
        
        Map<String, Object> templateVariables = createBookingTemplateVariables(booking);
        
        try {
            // Email de rappel
            if (booking.getUser().getEmail() != null) {
                emailService.sendTemplateEmail(
                    booking.getUser().getEmail(),
                    "Rappel - Votre rendez-vous approche",
                    "booking-reminder",
                    templateVariables
                );
            }
            
            // SMS de rappel
            if (booking.getUser().getPhone() != null) {
                String smsMessage = String.format(
                    "Rappel: RDV demain chez %s à %s. Réf: #%d",
                    booking.getBarber().getSalonName(),
                    booking.getBookingDate().toLocalTime(),
                    booking.getId()
                );
                smsService.sendSMS(booking.getUser().getPhone(), smsMessage);
            }
            
            // Push notification
            pushNotificationService.sendToUser(
                booking.getUser().getId(),
                "Rappel de rendez-vous",
                "N'oubliez pas votre rendez-vous demain"
                //createNotificationData(booking)
, null
            );
            
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des rappels", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    @Async
    public CompletableFuture<Void> sendBookingCancellation(Booking booking) {
        log.info("Envoi de notification d'annulation pour la réservation {}", booking.getId());
        
        Map<String, Object> templateVariables = createBookingTemplateVariables(booking);
        
        try {
            // Notifier le client
            if (booking.getUser().getEmail() != null) {
                emailService.sendTemplateEmail(
                    booking.getUser().getEmail(),
                    "Annulation de votre réservation",
                    "booking-cancellation",
                    templateVariables
                );
            }
            
            // Notifier le barbier
            if (booking.getBarber().getUser().getEmail() != null) {
                emailService.sendTemplateEmail(
                    booking.getBarber().getUser().getEmail(),
                    "Réservation annulée",
                    "booking-cancellation-barber",
                    templateVariables
                );
            }
            
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications d'annulation", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    @Async
    public CompletableFuture<Void> sendCustomNotification(NotificationDto notification) {
        log.info("Envoi de notification personnalisée de type {}", notification.getType());
        
        try {
            switch (notification.getType()) {
                case EMAIL -> emailService.sendSimpleEmail(
                    notification.getRecipient(),
                    notification.getSubject(),
                    notification.getMessage()
                );
                case SMS -> smsService.sendSMS(
                    notification.getRecipient(),
                    notification.getMessage()
                );
                case PUSH -> pushNotificationService.sendToUser(
                    Long.valueOf(notification.getRecipient()),
                    notification.getSubject(),
                    notification.getMessage(),
                    notification.getData()
                );
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification personnalisée", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    private Map<String, Object> createBookingTemplateVariables(Booking booking) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", booking.getUser().getFirstName() + " " + booking.getUser().getLastName());
        variables.put("barberName", booking.getBarber().getUser().getFirstName() + " " + booking.getBarber().getUser().getLastName());
        variables.put("salonName", booking.getBarber().getSalonName());
        variables.put("serviceName", booking.getBarberService().getName());
        variables.put("appointmentDate", booking.getBookingDate().toLocalDate());
        variables.put("appointmentTime", booking.getBookingTime());
        variables.put("duration", booking.getBarberService().getDuration());
        variables.put("price", booking.getBarberService().getPrice());
        variables.put("bookingId", booking.getId());
        variables.put("salonAddress", booking.getBarber().getLocation());
        return variables;
    }
    
    private Map<String, String> createNotificationData(Booking booking) {
        Map<String, String> data = new HashMap<>();
        data.put("bookingId", booking.getId().toString());
        data.put("type", "booking");
        data.put("barberId", booking.getBarber().getId().toString());
        return data;
    }
}