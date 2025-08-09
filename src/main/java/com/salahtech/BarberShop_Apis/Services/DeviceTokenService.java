package com.salahtech.BarberShop_Apis.Services;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.salahtech.BarberShop_Apis.models.DeviceToken;
import com.salahtech.BarberShop_Apis.reppsitories.DeviceTokenRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DeviceTokenService {

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    /**
     * Sauvegarder ou mettre à jour un token d'appareil
     */
    public void saveDeviceToken(Long userId, String token, String deviceType) {
        try {
            Optional<DeviceToken> existingToken = deviceTokenRepository.findByUserId(userId);
            
            if (existingToken.isPresent()) {
                DeviceToken deviceToken = existingToken.get();
                deviceToken.setToken(token);
                deviceToken.setDeviceType(deviceType);
                deviceToken.setActive(true);
                deviceTokenRepository.save(deviceToken);
                log.info("Device token updated for user: {}", userId);
            } else {
                DeviceToken newToken = new DeviceToken();
                newToken.setUserId(userId);
                newToken.setToken(token);
                newToken.setDeviceType(deviceType);
                newToken.setActive(true);
                deviceTokenRepository.save(newToken);
                log.info("New device token saved for user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to save device token for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Obtenir le token d'appareil d'un utilisateur
     */
    public String getDeviceToken(Long userId) {
        try {
            Optional<DeviceToken> deviceToken = deviceTokenRepository.findByUserIdAndActive(userId, true);
            return deviceToken.map(DeviceToken::getToken).orElse(null);
        } catch (Exception e) {
            log.error("Failed to get device token for user {}: {}", userId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Désactiver le token d'un utilisateur (lors de la déconnexion)
     */
    public void deactivateDeviceToken(Long userId) {
        try {
            Optional<DeviceToken> deviceToken = deviceTokenRepository.findByUserId(userId);
            if (deviceToken.isPresent()) {
                DeviceToken token = deviceToken.get();
                token.setActive(false);
                deviceTokenRepository.save(token);
                log.info("Device token deactivated for user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to deactivate device token for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Supprimer le token d'un utilisateur
     */
    public void deleteDeviceToken(Long userId) {
        try {
            Optional<DeviceToken> deviceToken = deviceTokenRepository.findByUserId(userId);
            if (deviceToken.isPresent()) {
                deviceTokenRepository.delete(deviceToken.get());
                log.info("Device token deleted for user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to delete device token for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Obtenir tous les tokens actifs (utile pour les notifications de masse)
     */
    public List<DeviceToken> getAllActiveTokens() {
        try {
            return deviceTokenRepository.findByActive(true);
        } catch (Exception e) {
            log.error("Failed to get all active device tokens: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Obtenir tous les tokens actifs d'un type d'appareil spécifique
     */
    public List<DeviceToken> getActiveTokensByDeviceType(String deviceType) {
        try {
            return deviceTokenRepository.findByDeviceTypeAndActive(deviceType, true);
        } catch (Exception e) {
            log.error("Failed to get active device tokens for device type {}: {}", deviceType, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Vérifier si un utilisateur a un token actif
     */
    public boolean hasActiveToken(Long userId) {
        try {
            return deviceTokenRepository.findByUserIdAndActive(userId, true).isPresent();
        } catch (Exception e) {
            log.error("Failed to check active token for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Mettre à jour uniquement le statut d'activité d'un token
     */
    public void updateTokenStatus(Long userId, boolean active) {
        try {
            Optional<DeviceToken> deviceToken = deviceTokenRepository.findByUserId(userId);
            if (deviceToken.isPresent()) {
                DeviceToken token = deviceToken.get();
                token.setActive(active);
                deviceTokenRepository.save(token);
                log.info("Device token status updated to {} for user: {}", active, userId);
            } else {
                log.warn("No device token found for user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to update token status for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Nettoyer les anciens tokens inactifs (utile pour maintenance)
     */
    public void cleanupInactiveTokens() {
        try {
            List<DeviceToken> inactiveTokens = deviceTokenRepository.findByActive(false);
            if (!inactiveTokens.isEmpty()) {
                deviceTokenRepository.deleteAll(inactiveTokens);
                log.info("Cleaned up {} inactive device tokens", inactiveTokens.size());
            }
        } catch (Exception e) {
            log.error("Failed to cleanup inactive tokens: {}", e.getMessage(), e);
        }
    }
}