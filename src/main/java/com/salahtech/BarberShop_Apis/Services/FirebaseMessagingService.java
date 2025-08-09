package com.salahtech.BarberShop_Apis.Services;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class FirebaseMessagingService {

    @Autowired
    private FirebaseMessaging firebaseMessaging;

    /**
     * Envoyer une notification à un seul appareil
     */
    public String sendNotification(String token, String title, String body, Map<String, String> data) {
        try {
            Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

            Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(notification);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();
            String response = firebaseMessaging.send(message);
            
            log.info("Successfully sent message: {}", response);
            return response;
            
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message to token {}: {}", token, e.getMessage(), e);
            throw new RuntimeException("Failed to send notification", e);
        }
    }

    /**
     * Envoyer une notification à plusieurs appareils
     */
    public BatchResponse sendMulticastNotification(List<String> tokens, String title, String body, Map<String, String> data) {
        try {
            Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(notification);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            MulticastMessage message = messageBuilder.build();
            BatchResponse response = firebaseMessaging.sendMulticast(message);
            
            log.info("Successfully sent {} messages, {} failed", 
                response.getSuccessCount(), response.getFailureCount());
            
            return response;
            
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send multicast FCM message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send multicast notification", e);
        }
    }

    /**
     * Envoyer une notification à un topic
     */
    public String sendToTopic(String topic, String title, String body, Map<String, String> data) {
        try {
            Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

            Message.Builder messageBuilder = Message.builder()
                .setTopic(topic)
                .setNotification(notification);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();
            String response = firebaseMessaging.send(message);
            
            log.info("Successfully sent message to topic {}: {}", topic, response);
            return response;
            
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message to topic {}: {}", topic, e.getMessage(), e);
            throw new RuntimeException("Failed to send notification to topic", e);
        }
    }

    /**
     * Souscrire des tokens à un topic
     */
    public void subscribeToTopic(List<String> tokens, String topic) {
        try {
            TopicManagementResponse response = firebaseMessaging.subscribeToTopic(tokens, topic);
            log.info("Successfully subscribed {} tokens to topic {}", response.getSuccessCount(), topic);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to subscribe to topic {}: {}", topic, e.getMessage(), e);
            throw new RuntimeException("Failed to subscribe to topic", e);
        }
    }

    /**
     * Désabonner des tokens d'un topic
     */
    public void unsubscribeFromTopic(List<String> tokens, String topic) {
        try {
            TopicManagementResponse response = firebaseMessaging.unsubscribeFromTopic(tokens, topic);
            log.info("Successfully unsubscribed {} tokens from topic {}", response.getSuccessCount(), topic);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to unsubscribe from topic {}: {}", topic, e.getMessage(), e);
            throw new RuntimeException("Failed to unsubscribe from topic", e);
        }
    }
}
