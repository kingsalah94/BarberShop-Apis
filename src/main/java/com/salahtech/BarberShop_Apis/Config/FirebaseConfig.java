package com.salahtech.BarberShop_Apis.Config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import lombok.extern.slf4j.Slf4j;

/**
 * Firebase Cloud Messaging (push).
 *
 * <p>Clé de compte de service cherchée dans cet ordre :</p>
 * <ol>
 *   <li>fichier pointé par {@code FIREBASE_CREDENTIALS_FILE} (Docker : volume/secret monté) ;</li>
 *   <li>ressource classpath {@code firebase.config.path} (dev local, fichier gitignoré).</li>
 * </ol>
 * <p>Sans clé, l'application démarre quand même (CI, image Docker sans secret) :
 * les envois push échouent et sont journalisés, le reste de l'API fonctionne.</p>
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials-file:${FIREBASE_CREDENTIALS_FILE:}}")
    private String credentialsFile;

    @Value("${firebase.config.path:firebase-service-account.json}")
    private String classpathConfig;

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(buildOptions());
        }
        return FirebaseMessaging.getInstance();
    }

    private FirebaseOptions buildOptions() throws IOException {
        if (credentialsFile != null && !credentialsFile.isBlank()) {
            Path path = Path.of(credentialsFile);
            if (Files.isReadable(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    log.info("Firebase initialisé depuis {}", path);
                    return FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(in)).build();
                }
            }
            log.warn("FIREBASE_CREDENTIALS_FILE={} illisible", path);
        }

        ClassPathResource resource = new ClassPathResource(classpathConfig);
        if (resource.exists()) {
            try (InputStream in = resource.getInputStream()) {
                log.info("Firebase initialisé depuis le classpath ({})", classpathConfig);
                return FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(in)).build();
            }
        }

        log.warn("Aucune clé Firebase trouvée : notifications push DÉSACTIVÉES "
                + "(définir FIREBASE_CREDENTIALS_FILE pour les activer)");
        return FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.create(new AccessToken("push-disabled", null)))
                .setProjectId("push-disabled")
                .build();
    }
}
