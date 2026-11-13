package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.QueueEntryDto;
import com.salahtech.BarberShop_Apis.notifications.UserNotifier;
import com.salahtech.BarberShop_Apis.models.QueueEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueNotifier {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserNotifier userNotifier;

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
        // Une seule fois par passage : la file est rediffusée à chaque mouvement.
        Long uid = clientUserId(entry);
        if (uid != null) userNotifier.notifyOnce(uid, "TURN_APPROACHING", title, body, entry.getId());
        sms(entry, body);
    }

    public void notifyTurnReady(QueueEntry entry) {
        String title = "C'est votre tour !";
        String body = String.format("%s, c'est à vous%s. Veuillez vous présenter.",
                entry.getCustomerName(),
                entry.getWorkstation() != null && entry.getWorkstation().getName() != null
                        ? " (" + entry.getWorkstation().getName() + ")" : "");
        Long uid = clientUserId(entry);
        if (uid != null) userNotifier.notify(uid, "TURN_READY", title, body, entry.getId());
        sms(entry, body);
    }

    /** Un client s'est enregistré lui-même dans la file (QR / appli) → le salon est prévenu. */
    public void notifyStaffCheckIn(QueueEntry entry) {
        try {
            userNotifier.notifyAll(UserNotifier.salonStaff(entry.getSalon(), entry.getPreferredEmployee()),
                    "QUEUE_CHECKIN", "Client arrivé",
                    entry.getCustomerName() + " est dans la file · ticket " + entry.getTicketNumber(), entry.getId());
        } catch (Exception e) {
            log.warn("Notif arrivée queueEntry={} échouée : {}", entry.getId(), e.getMessage());
        }
    }

    /** Encaissement enregistré → reçu au client. */
    public void notifyPaid(QueueEntry entry, boolean reward) {
        Long uid = clientUserId(entry);
        if (uid == null) return;
        String salon = entry.getSalon() != null ? entry.getSalon().getName() : "le salon";
        String body = reward
                ? "Votre visite offerte (fidélité) a été appliquée chez " + salon + "."
                : "Paiement de " + (entry.getAmount() != null ? entry.getAmount().stripTrailingZeros().toPlainString() : "?")
                        + " FCFA enregistré chez " + salon + ". Merci !";
        userNotifier.notify(uid, "PAYMENT_RECEIVED", reward ? "Visite offerte appliquée" : "Paiement enregistré", body, entry.getId());
    }

    /** Fin de prestation → remerciement (+ visite offerte débloquée le cas échéant). */
    public void notifyCompleted(QueueEntry entry, boolean rewardUnlocked) {
        Long uid = clientUserId(entry);
        if (uid == null) return;
        String salon = entry.getSalon() != null ? entry.getSalon().getName() : "le salon";
        userNotifier.notify(uid, "VISIT_COMPLETED", "Merci pour votre visite",
                "À bientôt chez " + salon + " ! Donnez votre avis depuis « Mes rendez-vous ».", entry.getId());
        if (rewardUnlocked) {
            userNotifier.notify(uid, "LOYALTY_REWARD", "Visite offerte débloquée 🎉",
                    "Votre carte fidélité chez " + salon + " est complète : votre prochaine visite est offerte.",
                    entry.getId());
        }
    }

    private static Long clientUserId(QueueEntry entry) {
        return entry.getClient() != null && entry.getClient().getUser() != null
                ? entry.getClient().getUser().getId() : null;
    }

    private void sms(QueueEntry entry, String body) {
        if (smsEnabled && entry.getCustomerPhone() != null && !entry.getCustomerPhone().isBlank()) {
            log.info("SMS [Twilio] -> {} : {}", entry.getCustomerPhone(), body);
        }
    }
}
