package com.salahtech.BarberShop_Apis.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Table(name = "device_tokens", 
       indexes = {
           @Index(name = "idx_user_id", columnList = "user_id"),
           @Index(name = "idx_token", columnList = "token"),
           @Index(name = "idx_active", columnList = "active"),
           @Index(name = "idx_user_active", columnList = "user_id, active")
       })
@Data
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User ID cannot be null")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotBlank(message = "Token cannot be blank")
    @Size(max = 1000, message = "Token cannot exceed 1000 characters")
    @Column(name = "token", nullable = false, length = 1000)
    private String token;

    @NotBlank(message = "Device type cannot be blank")
    @Size(max = 50, message = "Device type cannot exceed 50 characters")
    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType; // "ANDROID", "IOS", "WEB"

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Size(max = 100, message = "Device name cannot exceed 100 characters")
    @Column(name = "device_name", length = 100)
    private String deviceName; // "Samsung Galaxy S21", "iPhone 13", etc.

    @Size(max = 50, message = "Device OS version cannot exceed 50 characters")
    @Column(name = "device_os_version", length = 50)
    private String deviceOsVersion; // "Android 12", "iOS 15.1", etc.

    @Size(max = 100, message = "App version cannot exceed 100 characters")
    @Column(name = "app_version", length = 100)
    private String appVersion; // "1.2.3"

    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true;

    @Column(name = "marketing_enabled", nullable = false)
    private Boolean marketingEnabled = true;

    @Column(name = "booking_notifications_enabled", nullable = false)
    private Boolean bookingNotificationsEnabled = true;

    @Column(name = "promo_notifications_enabled", nullable = false)
    private Boolean promoNotificationsEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

     // Relation avec ApplicationUser (optionnel)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private ApplicationUser user;


    public DeviceToken() {
    }

    // Constructeur personnalisé pour les données essentielles
    public DeviceToken(Long userId, String token, String deviceType) {
        this.userId = userId;
        this.token = token;
        this.deviceType = deviceType;
        this.active = true;
        this.pushEnabled = true;
        this.marketingEnabled = true;
        this.bookingNotificationsEnabled = true;
        this.promoNotificationsEnabled = true;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setDeviceOsVersion(String deviceOsVersion) {
        this.deviceOsVersion = deviceOsVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public void setPushEnabled(Boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public void setMarketingEnabled(Boolean marketingEnabled) {
        this.marketingEnabled = marketingEnabled;
    }

    public void setBookingNotificationsEnabled(Boolean bookingNotificationsEnabled) {
        this.bookingNotificationsEnabled = bookingNotificationsEnabled;
    }

    public void setPromoNotificationsEnabled(Boolean promoNotificationsEnabled) {
        this.promoNotificationsEnabled = promoNotificationsEnabled;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public Boolean getActive() {
        return active;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceOsVersion() {
        return deviceOsVersion;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public Boolean getPushEnabled() {
        return pushEnabled;
    }

    public Boolean getMarketingEnabled() {
        return marketingEnabled;
    }

    public Boolean getBookingNotificationsEnabled() {
        return bookingNotificationsEnabled;
    }

    public Boolean getPromoNotificationsEnabled() {
        return promoNotificationsEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;



    // Méthodes utilitaires
    public boolean isAndroid() {
        return "ANDROID".equalsIgnoreCase(this.deviceType);
    }

    public boolean isIOS() {
        return "IOS".equalsIgnoreCase(this.deviceType);
    }

    public boolean isWeb() {
        return "WEB".equalsIgnoreCase(this.deviceType);
    }

    public boolean canReceiveNotifications() {
        return this.active && this.pushEnabled;
    }

    public boolean canReceiveMarketingNotifications() {
        return canReceiveNotifications() && this.marketingEnabled;
    }

    public boolean canReceiveBookingNotifications() {
        return canReceiveNotifications() && this.bookingNotificationsEnabled;
    }

    public boolean canReceivePromoNotifications() {
        return canReceiveNotifications() && this.promoNotificationsEnabled;
    }

    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    // Énumération pour les types d'appareils
    public enum DeviceType {
        ANDROID("ANDROID"),
        IOS("IOS"), 
        WEB("WEB");

        private final String value;

        DeviceType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static DeviceType fromString(String value) {
            for (DeviceType type : DeviceType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid device type: " + value);
        }
    }

    // Méthode toString personnalisée pour le logging (sans exposer le token complet)
    @Override
    public String toString() {
        return "DeviceToken{" +
                "id=" + id +
                ", userId=" + userId +
                ", token='" + (token != null ? token.substring(0, Math.min(token.length(), 20)) + "..." : null) + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", active=" + active +
                ", deviceName='" + deviceName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
