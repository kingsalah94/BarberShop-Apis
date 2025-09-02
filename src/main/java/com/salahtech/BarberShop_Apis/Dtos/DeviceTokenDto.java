package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.models.DeviceToken;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenDto {
    private Long id;
    private Long userId;
    private String token;
    private String deviceType;
    private Boolean active;
    private String deviceName;
    private String deviceOsVersion;
    private String appVersion;
    private Boolean pushEnabled;
    private Boolean marketingEnabled;
    private Boolean bookingNotificationsEnabled;
    private Boolean promoNotificationsEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastUsedAt;

    public static DeviceTokenDto fromEntity(DeviceToken e) {
        if (e == null)
            return null;
        return DeviceTokenDto.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .token(e.getToken())
                .deviceType(e.getDeviceType())
                .active(e.getActive())
                .deviceName(e.getDeviceName())
                .deviceOsVersion(e.getDeviceOsVersion())
                .appVersion(e.getAppVersion())
                .pushEnabled(e.getPushEnabled())
                .marketingEnabled(e.getMarketingEnabled())
                .bookingNotificationsEnabled(e.getBookingNotificationsEnabled())
                .promoNotificationsEnabled(e.getPromoNotificationsEnabled())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .lastUsedAt(e.getLastUsedAt())
                .build();
    }
}
