package com.salahtech.BarberShop_Apis.notifications;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.salahtech.BarberShop_Apis.Services.Interfaces.DeviceTokenService;
import com.salahtech.BarberShop_Apis.models.Employee;
import com.salahtech.BarberShop_Apis.models.Notification;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.repositories.NotificationRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Point d'entrée unique des notifications « utilisateur ».
 *
 * <p>Pour chaque notification : 1) enregistrement en base (cloche web / mobile),
 * 2) diffusion temps réel STOMP sur {@code /topic/user/{id}/notifications},
 * 3) push FCM si l'utilisateur a un appareil enregistré.</p>
 *
 * <p>Tout part <b>après le commit</b> de la transaction appelante (rien n'est annoncé
 * si l'opération échoue) et sur un pool dédié : l'appel HTTP ne paie pas le coût FCM.
 * Les appelants passent des valeurs simples (ids, textes) calculées dans leur transaction.</p>
 */
@Slf4j
@Component
public class UserNotifier {

    private static final int MAX_LEN = 255;

    private final NotificationRepository repository;
    private final SimpMessagingTemplate ws;
    private final DeviceTokenService deviceTokens;
    private final FirebaseMessaging fcm;
    private final TransactionTemplate newTx;
    private final ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "user-notifier");
        t.setDaemon(true);
        return t;
    });

    public UserNotifier(NotificationRepository repository, SimpMessagingTemplate ws,
            DeviceTokenService deviceTokens, FirebaseMessaging fcm, PlatformTransactionManager tm) {
        this.repository = repository;
        this.ws = ws;
        this.deviceTokens = deviceTokens;
        this.fcm = fcm;
        this.newTx = new TransactionTemplate(tm);
        this.newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Notification enregistrée + temps réel + push. */
    public void notify(Long userId, String type, String title, String body, Long referenceId) {
        schedule(userId, type, title, body, referenceId, true, false, null);
    }

    /** Idem, mais une seule fois par (utilisateur, type, référence) — ex. « votre tour approche ». */
    public void notifyOnce(Long userId, String type, String title, String body, Long referenceId) {
        schedule(userId, type, title, body, referenceId, true, true, null);
    }

    /** Push seul (pas de ligne dans la cloche) — ex. nouveau message, qui a déjà son propre badge. */
    public void pushOnly(Long userId, String type, String title, String body, Map<String, String> data) {
        schedule(userId, type, title, body, null, false, false, data);
    }

    /** Même notification à plusieurs destinataires (sans doublon). */
    public void notifyAll(Set<Long> userIds, String type, String title, String body, Long referenceId) {
        for (Long id : userIds) notify(id, type, title, body, referenceId);
    }

    /**
     * Personnel à prévenir pour un salon : le propriétaire et, le cas échéant,
     * le prestataire concerné (s'il a un compte). À appeler dans la transaction.
     */
    public static Set<Long> salonStaff(Salon salon, Employee concerned) {
        Set<Long> ids = new LinkedHashSet<>();
        if (salon != null && salon.getOwner() != null) ids.add(salon.getOwner().getId());
        if (concerned != null && concerned.getUser() != null) ids.add(concerned.getUser().getId());
        return ids;
    }

    // ------------------------------------------------------------------

    private void schedule(Long userId, String type, String title, String body, Long ref,
            boolean persist, boolean once, Map<String, String> data) {
        if (userId == null) return;
        Runnable job = () -> pool.execute(() -> deliver(userId, type, cut(title), cut(body), ref, persist, once, data));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    job.run();
                }
            });
        } else {
            job.run();
        }
    }

    private void deliver(Long userId, String type, String title, String body, Long ref,
            boolean persist, boolean once, Map<String, String> data) {
        Long notifId = null;
        LocalDateTime createdAt = LocalDateTime.now();
        if (persist) {
            try {
                Notification saved = newTx.execute(s -> {
                    if (once && ref != null && repository.existsByUserIdAndTypeAndReferenceId(userId, type, ref)) {
                        return null;
                    }
                    return repository.save(Notification.builder()
                            .userId(userId).title(title).body(body).type(type)
                            .referenceId(ref).read(false).build());
                });
                if (saved == null) return; // déjà notifié
                notifId = saved.getId();
                if (saved.getCreatedAt() != null) createdAt = saved.getCreatedAt();
            } catch (Exception e) {
                log.warn("Notification {} non enregistrée pour user {} : {}", type, userId, e.getMessage());
            }
        }

        // Temps réel (web + mobile ouverts).
        try {
            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("id", notifId);
            ev.put("type", type);
            ev.put("title", title);
            ev.put("body", body);
            ev.put("referenceId", ref);
            ev.put("read", false);
            ev.put("createdAt", createdAt.toString());
            ev.put("persisted", persist);
            ws.convertAndSend("/topic/user/" + userId + "/notifications", ev);
        } catch (Exception e) {
            log.debug("Diffusion WS notification échouée : {}", e.getMessage());
        }

        push(userId, type, title, body, ref, notifId, data);
    }

    private void push(Long userId, String type, String title, String body, Long ref, Long notifId,
            Map<String, String> extra) {
        String token;
        try {
            token = deviceTokens.getDeviceToken(userId);
        } catch (Exception e) {
            return;
        }
        if (token == null || token.isBlank()) return;
        Map<String, String> data = new HashMap<>();
        if (extra != null) data.putAll(extra);
        data.put("type", type);
        if (ref != null) data.put("referenceId", String.valueOf(ref));
        if (notifId != null) data.put("notificationId", String.valueOf(notifId));
        try {
            fcm.send(Message.builder()
                    .setToken(token)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title).setBody(body).build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .putAllData(data)
                    .build());
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                    || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                log.info("Jeton FCM périmé pour user {} : désactivé", userId);
                deviceTokens.deactivateDeviceToken(userId);
            } else {
                log.warn("Push FCM {} vers user {} échoué : {}", type, userId, e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Push FCM {} vers user {} échoué : {}", type, userId, e.getMessage());
        }
    }

    private static String cut(String s) {
        if (s == null) return null;
        return s.length() <= MAX_LEN ? s : s.substring(0, MAX_LEN - 1) + "…";
    }
}
