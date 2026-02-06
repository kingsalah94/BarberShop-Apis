package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.AOP.LogActivity;
import com.salahtech.BarberShop_Apis.Dtos.DeviceTokenDto;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.DeviceTokenService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.DeviceToken;
import com.salahtech.BarberShop_Apis.models.DeviceToken.DeviceType;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.reppsitories.DeviceTokenRepository;
import com.salahtech.BarberShop_Apis.specs.DeviceTokenSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final ApplicationUserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<DeviceTokenDto> search(Long userId, Boolean active, Set<String> types,
            LocalDateTime updatedBefore, Pageable pageable) {
        Specification<DeviceToken> spec = Specification
                .where(DeviceTokenSpecifications.byUserId(userId))
                .and(DeviceTokenSpecifications.active(active))
                .and(DeviceTokenSpecifications.deviceTypesIn(types))
                .and(DeviceTokenSpecifications.updatedBefore(updatedBefore));

        return deviceTokenRepository.findAll(spec, pageable).map(DeviceTokenDto::fromEntity);
    }

    // @Override
    // public DeviceTokenDto register(DeviceTokenDto dto) {
    // // upsert par token
    // DeviceToken entity =
    // deviceTokenRepository.findByToken(dto.getToken()).orElseGet(DeviceToken::new);
    // entity.setUserId(dto.getUserId());
    // entity.setToken(dto.getToken());
    // entity.setDeviceType(dto.getDeviceType());
    // entity.setActive(dto.getActive() != null ? dto.getActive() : Boolean.TRUE);
    // entity.setDeviceName(dto.getDeviceName());
    // entity.setDeviceOsVersion(dto.getDeviceOsVersion());
    // entity.setAppVersion(dto.getAppVersion());
    // entity.setPushEnabled(dto.getPushEnabled() != null ? dto.getPushEnabled() :
    // Boolean.TRUE);
    // entity.setMarketingEnabled(dto.getMarketingEnabled() != null ?
    // dto.getMarketingEnabled() : Boolean.TRUE);
    // entity.setBookingNotificationsEnabled(
    // dto.getBookingNotificationsEnabled() != null ?
    // dto.getBookingNotificationsEnabled() : Boolean.TRUE);
    // entity.setPromoNotificationsEnabled(
    // dto.getPromoNotificationsEnabled() != null ?
    // dto.getPromoNotificationsEnabled() : Boolean.TRUE);
    // entity.updateLastUsed();

    // return DeviceTokenDto.fromEntity(deviceTokenRepository.save(entity));
    // }

    @Override
    public DeviceTokenDto activate(Long id, boolean value) {
        var e = deviceTokenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeviceToken not found"));
        e.setActive(value);
        return DeviceTokenDto.fromEntity(deviceTokenRepository.save(e));
    }

    @Override
    public void delete(Long id) {
        if (!deviceTokenRepository.existsById(id))
            throw new ResourceNotFoundException("DeviceToken not found");
        deviceTokenRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceTokenDto findById(Long id) {
        return deviceTokenRepository.findById(id).map(DeviceTokenDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("DeviceToken not found"));
    }

    /**
     * Upsert du token pour un utilisateur.
     * - On résout l'utilisateur;
     * - on cherche un token existant (par user et actif) pour mise à jour;
     * - sinon on en crée un nouveau.
     */
    @Override
    @LogActivity(action = "CREATE_DEVICE_TOKEN", entityName = "DeviceToken")
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
    @Override
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
    @Override
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

    @Override
    public void deleteDeviceTokens(Long userId) {
        try {
            deviceTokenRepository.deleteByUser_Id(userId);
            log.info("Device tokens deleted for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete device tokens for user {}: {}", userId, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceToken> getAllActiveTokens() {
        try {
            return deviceTokenRepository.findByActive(true);
        } catch (Exception e) {
            log.error("Failed to get all active device tokens: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceToken> getActiveTokensByDeviceType(String deviceType) {
        try {
            return deviceTokenRepository.findByDeviceTypeAndActive(deviceType, true);
        } catch (Exception e) {
            log.error("Failed to get active device tokens for device type {}: {}", deviceType, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveToken(Long userId) {
        try {
            return deviceTokenRepository.findByUser_IdAndActive(userId, true).isPresent();
        } catch (Exception e) {
            log.error("Failed to check active token for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
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

    @Override
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

    // @Override
    // @Transactional
    // public DeviceTokenDto register(DeviceTokenDto dto) {
    // if (dto == null || dto.getToken() == null || dto.getToken().isBlank()) {
    // throw new IllegalArgumentException("Le token ne peut pas être vide");
    // }

    // // Vérifier si le token existe déjà
    // Optional<DeviceToken> existing =
    // deviceTokenRepository.findByToken(dto.getToken());

    // DeviceToken entity;
    // if (existing.isPresent()) {
    // entity = existing.get();
    // entity.setActive(true);
    // if (dto.getUserId() != null) {
    // entity.setUserId(dto.getUserId());
    // }
    // if (dto.getDeviceType() != null) {
    // try {
    // entity.setDeviceType(DeviceToken.DeviceType.valueOf(dto.getDeviceType().toUpperCase()));
    // } catch (IllegalArgumentException ex) {
    // log.warn("Type d’appareil inconnu : {}. Valeurs valides = ANDROID, IOS, WEB",
    // dto.getDeviceType());
    // }
    // }
    // } else {
    // entity = new DeviceToken();
    // entity.setToken(dto.getToken());
    // entity.setActive(true);
    // entity.setUserId(dto.getUserId());
    // if (dto.getDeviceType() != null) {
    // try {
    // entity.setDeviceType(DeviceToken.DeviceType.valueOf(dto.getDeviceType().toUpperCase()));
    // } catch (IllegalArgumentException ex) {
    // log.warn("Type d’appareil inconnu : {}. Valeurs valides = ANDROID, IOS, WEB",
    // dto.getDeviceType());
    // }
    // }
    // }

    // entity.setUpdatedAt(LocalDateTime.now());
    // DeviceToken saved = deviceTokenRepository.save(entity);

    // return DeviceTokenDto.fromEntity(saved);
    // }

}
