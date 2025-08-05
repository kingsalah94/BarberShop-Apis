package com.salahtech.BarberShop_Apis.Utils;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
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
    
    public boolean tryConsume(String key, String operation) {
        return getBucket(key, operation).tryConsume(1);
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
