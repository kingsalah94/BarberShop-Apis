package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.DeviceTokenDto;
import com.salahtech.BarberShop_Apis.models.DeviceToken;
import com.salahtech.BarberShop_Apis.models.DeviceToken.DeviceType;

import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface DeviceTokenService {
    Page<DeviceTokenDto> search(Long userId, Boolean active, Set<String> types, LocalDateTime updatedBefore,
            Pageable pageable);

    // DeviceTokenDto register(DeviceTokenDto dto); // upsert par token

    DeviceTokenDto activate(Long id, boolean value);

    void delete(Long id);

    DeviceTokenDto findById(Long id);

    void updateTokenStatus(Long userId, boolean active);

    void cleanupInactiveTokens();

    boolean hasActiveToken(Long userId);

    List<DeviceToken> getActiveTokensByDeviceType(String deviceType);

    List<DeviceToken> getAllActiveTokens();

    /** Supprime tous les tokens du user (si tu veux un clean complet) */
    void deleteDeviceTokens(Long userId);

    /** Désactive le token actif du user */
    void deactivateDeviceToken(Long userId);

    /**
     * Récupère le token actif courant (si tu n’autorises qu’un device actif/user)
     */
    String getDeviceToken(Long userId);

    /**
     * Upsert du token pour un utilisateur.
     * - On résout l'utilisateur;
     * - on cherche un token existant (par user et actif) pour mise à jour;
     * - sinon on en crée un nouveau.
     */
    void saveDeviceToken(Long userId, String token, DeviceType deviceType);

    // Object register(DeviceTokenDto dto);
}
