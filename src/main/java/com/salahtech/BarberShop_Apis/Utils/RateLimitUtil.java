package com.salahtech.BarberShop_Apis.Utils;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitUtil {
    
    @Value("${rate-limit.login.capacity:5}")
    private int loginCapacity;
    
    @Value("${rate-limit.login.refill-tokens:1}")
    private int loginRefillTokens;
    
    @Value("${rate-limit.login.refill-period:300}")
    private int loginRefillPeriodSeconds;
    
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /** Paramètres › Sécurité : « Connexions / min » s'applique à l'opération login. */
    @Autowired(required = false)
    private ObjectProvider<com.salahtech.BarberShop_Apis.Services.Interfaces.SecuritySettingsService> securitySettings;

    public boolean tryConsume(String key, String operation) {
        if ("login".equals(operation)) {
            int perMinute = loginPerMinute();
            if (perMinute > 0) {
                // La valeur fait partie de la clé : un changement de réglage repart sur un seau neuf.
                return cache.computeIfAbsent("login:" + perMinute + ":" + key,
                        k -> Bucket4j.builder().addLimit(Bandwidth.classic(perMinute,
                                Refill.greedy(perMinute, Duration.ofMinutes(1)))).build())
                        .tryConsume(1);
            }
        }
        return getBucket(key, operation).tryConsume(1);
    }

    private int loginPerMinute() {
        try {
            var svc = securitySettings == null ? null : securitySettings.getIfAvailable();
            Integer v = svc == null ? null : svc.get().getRateLoginPerMinute();
            return v == null ? 0 : Math.min(Math.max(v, 1), 1000);
        } catch (RuntimeException e) {
            return 0; // réglage indisponible : configuration par défaut
        }
    }
    
    private Bucket getBucket(String key, String operation) {
        String bucketKey = operation + ":" + key;
        return cache.computeIfAbsent(bucketKey, this::newBucket);
    }
    
    private Bucket newBucket(String key) {
        // Configuration par défaut pour les tentatives de connexion
        Bandwidth limit = Bandwidth.classic(loginCapacity, 
                Refill.intervally(loginRefillTokens, Duration.ofSeconds(loginRefillPeriodSeconds)));
        
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }
}
