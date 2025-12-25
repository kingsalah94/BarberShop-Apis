package com.salahtech.BarberShop_Apis.Config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.path:firebase-service-account.json}")
    private String firebaseConfigPath;

    @PostConstruct
    public void initialize() {
        try {
            // Vérifie si Firebase est déjà initialisé pour éviter les erreurs au
            // redémarrage à chaud
            if (FirebaseApp.getApps().isEmpty()) {
                // Charge le fichier depuis le dossier resources/
                ClassPathResource resource = new ClassPathResource(firebaseConfigPath);

                if (!resource.exists()) {
                    System.err.println("⚠️ ATTENTION : Le fichier firebase-service-account.json est introuvable !");
                    return;
                }

                try (InputStream serviceAccount = resource.getInputStream()) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();

                    FirebaseApp.initializeApp(options);
                    System.out.println("✅ Firebase Application Initialized");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}
