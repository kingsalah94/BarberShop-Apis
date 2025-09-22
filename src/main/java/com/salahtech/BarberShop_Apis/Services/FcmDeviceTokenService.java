package com.salahtech.BarberShop_Apis.Services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.DeviceToken;
import com.salahtech.BarberShop_Apis.models.DeviceToken.DeviceType;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.reppsitories.DeviceTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FcmDeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final ApplicationUserRepository userRepository;

    /**
     * Upsert du token pour un utilisateur.
     * - On résout l'utilisateur;
     * - on cherche un token existant (par user et actif) pour mise à jour;
     * - sinon on en crée un nouveau.
     */
    public void saveDeviceToken(Long userId, String token, DeviceType deviceType) {
        try {
            ApplicationUser user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // 1) Si ce token existe déjà → on l’attache à l’utilisateur (idempotent)
            Optional<DeviceToken> sameToken = deviceTokenRepository.findByToken(token);
            if (sameToken.isPresent()) {
                DeviceToken dt = sameToken.get();
                dt.setUser(user);
                dt.setDeviceType(deviceType);
                dt.setActive(true);
                dt.updateLastUsed();
                deviceTokenRepository.save(dt);
                log.info("Device token reassigned/updated for user {}", userId);
                return;
            }

            // 2) Sinon, on upsert le token actif du user (optionnel: autoriser plusieurs
            // devices/user)
            Optional<DeviceToken> existingActive = deviceTokenRepository.findByUser_IdAndActive(userId, true);
            if (existingActive.isPresent()) {
                DeviceToken dt = existingActive.get();
                dt.setToken(token);
                dt.setDeviceType(deviceType);
                dt.setActive(true);
                dt.updateLastUsed();
                deviceTokenRepository.save(dt);
                log.info("Device token updated for user {}", userId);
            } else {
                DeviceToken dt = new DeviceToken();
                dt.setUser(user);
                dt.setToken(token);
                dt.setDeviceType(deviceType);
                dt.setActive(true);
                dt.updateLastUsed();
                deviceTokenRepository.save(dt);
                log.info("New device token saved for user {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to save device token for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Récupère le token actif courant (si tu n’autorises qu’un device actif/user)
     */
    @Transactional(readOnly = true)
    public String getDeviceToken(Long userId) {
        try {
            return deviceTokenRepository.findByUser_IdAndActive(userId, true)
                    .map(DeviceToken::getToken)
                    .orElse(null);
        } catch (Exception e) {
            log.error("Failed to get device token for user {}: {}", userId, e.getMessage(), e);
            return null;
        }
    }

    /** Désactive le token actif du user */
    public void deactivateDeviceToken(Long userId) {
        try {
            deviceTokenRepository.findByUser_Id(userId).ifPresent(dt -> {
                dt.setActive(false);
                deviceTokenRepository.save(dt);
                log.info("Device token deactivated for user {}", userId);
            });
        } catch (Exception e) {
            log.error("Failed to deactivate device token for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /** Supprime tous les tokens du user (si tu veux un clean complet) */
    public void deleteDeviceTokens(Long userId) {
        try {
            deviceTokenRepository.deleteByUser_Id(userId);
            log.info("Device tokens deleted for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete device tokens for user {}: {}", userId, e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<DeviceToken> getAllActiveTokens() {
        try {
            return deviceTokenRepository.findByActive(true);
        } catch (Exception e) {
            log.error("Failed to get all active device tokens: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<DeviceToken> getActiveTokensByDeviceType(String deviceType) {
        try {
            return deviceTokenRepository.findByDeviceTypeAndActive(deviceType, true);
        } catch (Exception e) {
            log.error("Failed to get active device tokens for device type {}: {}", deviceType, e.getMessage(), e);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public boolean hasActiveToken(Long userId) {
        try {
            return deviceTokenRepository.findByUser_IdAndActive(userId, true).isPresent();
        } catch (Exception e) {
            log.error("Failed to check active token for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    public void updateTokenStatus(Long userId, boolean active) {
        try {
            deviceTokenRepository.findByUser_Id(userId).ifPresent(dt -> {
                dt.setActive(active);
                deviceTokenRepository.save(dt);
                log.info("Device token status updated to {} for user {}", active, userId);
            });
        } catch (Exception e) {
            log.error("Failed to update token status for user {}: {}", userId, e.getMessage(), e);
        }
    }

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
