package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.DeviceTokenDto;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.DeviceTokenService;
import com.salahtech.BarberShop_Apis.models.DeviceToken;
import com.salahtech.BarberShop_Apis.reppsitories.DeviceTokenRepository;
import com.salahtech.BarberShop_Apis.specs.DeviceTokenSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final DeviceTokenRepository repo;

    @Override
    @Transactional(readOnly = true)
    public Page<DeviceTokenDto> search(Long userId, Boolean active, Set<String> types,
            LocalDateTime updatedBefore, Pageable pageable) {
        Specification<DeviceToken> spec = Specification
                .where(DeviceTokenSpecifications.byUserId(userId))
                .and(DeviceTokenSpecifications.active(active))
                .and(DeviceTokenSpecifications.deviceTypesIn(types))
                .and(DeviceTokenSpecifications.updatedBefore(updatedBefore));

        return repo.findAll(spec, pageable).map(DeviceTokenDto::fromEntity);
    }

    @Override
    public DeviceTokenDto register(DeviceTokenDto dto) {
        // upsert par token
        DeviceToken entity = repo.findByToken(dto.getToken()).orElseGet(DeviceToken::new);
        entity.setUserId(dto.getUserId());
        entity.setToken(dto.getToken());
        entity.setDeviceType(dto.getDeviceType());
        entity.setActive(dto.getActive() != null ? dto.getActive() : Boolean.TRUE);
        entity.setDeviceName(dto.getDeviceName());
        entity.setDeviceOsVersion(dto.getDeviceOsVersion());
        entity.setAppVersion(dto.getAppVersion());
        entity.setPushEnabled(dto.getPushEnabled() != null ? dto.getPushEnabled() : Boolean.TRUE);
        entity.setMarketingEnabled(dto.getMarketingEnabled() != null ? dto.getMarketingEnabled() : Boolean.TRUE);
        entity.setBookingNotificationsEnabled(
                dto.getBookingNotificationsEnabled() != null ? dto.getBookingNotificationsEnabled() : Boolean.TRUE);
        entity.setPromoNotificationsEnabled(
                dto.getPromoNotificationsEnabled() != null ? dto.getPromoNotificationsEnabled() : Boolean.TRUE);
        entity.updateLastUsed();

        return DeviceTokenDto.fromEntity(repo.save(entity));
    }

    @Override
    public DeviceTokenDto activate(Long id, boolean value) {
        var e = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("DeviceToken not found"));
        e.setActive(value);
        return DeviceTokenDto.fromEntity(repo.save(e));
    }

    @Override
    public void delete(Long id) {
        if (!repo.existsById(id))
            throw new ResourceNotFoundException("DeviceToken not found");
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceTokenDto findById(Long id) {
        return repo.findById(id).map(DeviceTokenDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("DeviceToken not found"));
    }
}
