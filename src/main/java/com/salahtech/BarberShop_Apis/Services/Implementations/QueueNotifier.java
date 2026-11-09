package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.QueueEntryDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.DeviceTokenService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.NotificationService;
import com.salahtech.BarberShop_Apis.models.QueueEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueNotifier {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final DeviceTokenService deviceTokenService;

    @Value("${sms.enabled:false}")
    private boolean smsEnabled;

    public void broadcastQueueUpdate(Long salonId, List<QueueEntryDto> queue) {
        String dest = "/topic/salon/" + salonId + "/queue";
        messagingTemplate.convertAndSend(dest, queue);
        log.debug("Broadcast queue update to {} ({} entries)", dest, queue.size());
    }

    public void broadcastEntryUpdate(Long salonId, QueueEntryDto entry) {
        String dest = "/topic/salon/" + salonId + "/queue/entry";
        messagingTemplate.convertAndSend(dest, entry);
    }

    public void notifyTurnApproaching(QueueEntry entry, int position) {
        String title = "Votre tour approche";
        String body = String.format("Bonjour %s, votre tour arrive bientôt (position %d). Veuillez vous préparer.",
                entry.getCustomerName(), position);
        notify(entry, title, body, "TURN_APPROACHING");
    }

    public void notifyTurnReady(QueueEntry entry) {
        String title = "C'est votre tour !";
        String body = String.format("%s, c'est à vous. Veuillez vous présenter à l'accueil.",
                entry.getCustomerName());
        notify(entry, title, body, "TURN_READY");
    }

    private void notify(QueueEntry entry, String title, String body, String type) {
        try {
            if (entry.getClient() != null
                    && entry.getClient().getUser() != null
                    && deviceTokenService.hasActiveToken(entry.getClient().getUser().getId())) {
                String token = deviceTokenService.getDeviceToken(entry.getClient().getUser().getId());
                if (token != null && !token.isBlank()) {
                    Map<String, String> data = new HashMap<>();
                    data.put("type", type);
                    data.put("queueEntryId", String.valueOf(entry.getId()));
                    data.put("ticketNumber", entry.getTicketNumber());
                    notificationService.sendNotification(token, title, body, data);
                    log.info("FCM push envoyé queueEntry={} type={}", entry.getId(), type);
                }
            }
        } catch (Exception ex) {
            log.warn("Échec envoi FCM pour queueEntry={}: {}", entry.getId(), ex.getMessage());
        }

        if (smsEnabled && entry.getCustomerPhone() != null && !entry.getCustomerPhone().isBlank()) {
            log.info("SMS [Twilio] -> {} : {}", entry.getCustomerPhone(), body);
        } else if (entry.getCustomerPhone() != null) {
            log.debug("SMS désactivé, skip pour {}", entry.getCustomerPhone());
        }
    }
}