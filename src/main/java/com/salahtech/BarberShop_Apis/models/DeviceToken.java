package com.salahtech.BarberShop_Apis.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "device_tokens", indexes = {
        @Index(name = "idx_dt_user", columnList = "user_id"),
        @Index(name = "idx_dt_active", columnList = "active"),
        @Index(name = "idx_dt_last_used", columnList = "last_used_at")
}, uniqueConstraints = {
        // un utilisateur ne doit pas avoir deux fois le même token
        @UniqueConstraint(name = "uk_dt_user_token", columnNames = { "user_id", "token" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Plusieurs tokens par utilisateur */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private ApplicationUser user;

    /** Token push (FCM/APNs) */
    @NotBlank(message = "Token cannot be blank")
    @Size(max = 1000, message = "Token cannot exceed 1000 characters")
    @Column(name = "token", nullable = false, length = 1000)
    private String token;

    /** ANDROID / IOS / WEB */
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Size(max = 100)
    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Size(max = 50)
    @Column(name = "device_os_version", length = 50)
    private String deviceOsVersion;

    @Size(max = 100)
    @Column(name = "app_version", length = 100)
    private String appVersion;

    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private Boolean pushEnabled = true;

    @Column(name = "marketing_enabled", nullable = false)
    @Builder.Default
    private Boolean marketingEnabled = true;

    @Column(name = "booking_notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean bookingNotificationsEnabled = true;

    @Column(name = "promo_notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean promoNotificationsEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /** -------- Helpers / Validation -------- */

    @PrePersist
    @PreUpdate
    private void normalize() {
        if (token != null)
            token = token.trim();
        if (active == null)
            active = true;
        if (pushEnabled == null)
            pushEnabled = true;
        if (marketingEnabled == null)
            marketingEnabled = true;
        if (bookingNotificationsEnabled == null)
            bookingNotificationsEnabled = true;
        if (promoNotificationsEnabled == null)
            promoNotificationsEnabled = true;
        if (deviceType == null) {
            throw new IllegalArgumentException("DeviceToken invalid: deviceType is required");
        }
    }

    public boolean isAndroid() {
        return deviceType == DeviceType.ANDROID;
    }

    public boolean isIOS() {
        return deviceType == DeviceType.IOS;
    }

    public boolean isWeb() {
        return deviceType == DeviceType.WEB;
    }

    public boolean canReceiveNotifications() {
        return Boolean.TRUE.equals(active) && Boolean.TRUE.equals(pushEnabled);
    }

    public boolean canReceiveMarketingNotifications() {
        return canReceiveNotifications() && Boolean.TRUE.equals(marketingEnabled);
    }

    public boolean canReceiveBookingNotifications() {
        return canReceiveNotifications() && Boolean.TRUE.equals(bookingNotificationsEnabled);
    }

    public boolean canReceivePromoNotifications() {
        return canReceiveNotifications() && Boolean.TRUE.equals(promoNotificationsEnabled);
    }

    public void markUsedNow() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
    }

    /** Masquage du token dans les logs */
    @Override
    public String toString() {
        String masked = token != null ? token.substring(0, Math.min(20, token.length())) + "..." : null;
        return "DeviceToken{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", token='" + masked + '\'' +
                ", deviceType=" + deviceType +
                ", active=" + active +
                ", deviceName='" + deviceName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    public enum DeviceType {
        ANDROID, IOS, WEB
    }
}
