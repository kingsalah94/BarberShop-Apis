package com.salahtech.BarberShop_Apis.Dtos;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for complete Client Profile information
 * Used for displaying client profile and account details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientProfileDto {

    // ===== BASIC INFORMATION =====

    /**
     * Client ID
     */
    private Long id;

    private Long userId;

    /**
     * First name
     */
    private String firstName;

    /**
     * Last name
     */
    private String lastName;

    /**
     * Full name (computed)
     */
    private String fullName;

    /**
     * Email address
     */
    private String email;

    /**
     * Phone number
     */
    private String phoneNumber;

    private LocalDateTime birthDate;
    private String preferences;

    // Statistics

    private Long completedBookingsCount;

    private Long upcomingBookingsCount;

    private Integer favoritesCount;

    // Timestamps
    private LocalDateTime memberSince;
    private LocalDateTime lastUpdated;

    /**
     * Profile photo URL
     */
    private String profilePictureUrl;

    // ===== ADDRESS INFORMATION =====

    /**
     * Street address
     */
    private String address;

    /**
     * City
     */
    private String city;

    /**
     * Postal code
     */
    private String postalCode;

    /**
     * Country
     */
    private String country;

    /**
     * Full formatted address
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (address != null && !address.isEmpty()) {
            sb.append(address);
        }
        if (city != null && !city.isEmpty()) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(city);
        }
        if (postalCode != null && !postalCode.isEmpty()) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(postalCode);
        }
        if (country != null && !country.isEmpty()) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(country);
        }
        return sb.toString();
    }

    // ===== ACCOUNT STATUS =====

    /**
     * Is email verified?
     */
    private Boolean emailVerified;

    /**
     * Is phone verified?
     */
    private Boolean phoneVerified;

    /**
     * Account active status
     */
    private Boolean isActive;

    /**
     * Account creation date
     */

    private LocalDateTime createdAt;

    /**
     * Last login date
     */
    private LocalDateTime lastLoginAt;

    // ===== LOYALTY PROGRAM =====

    /**
     * Current loyalty points balance
     */
    private Integer loyaltyPoints;

    /**
     * Loyalty tier (BRONZE, SILVER, GOLD, PLATINUM)
     */
    private String loyaltyTier;

    /**
     * Points to next tier
     */
    private Integer pointsToNextTier;

    /**
     * Total lifetime points earned
     */
    private Integer lifetimePoints;

    // ===== STATISTICS =====

    /**
     * Total number of bookings (all statuses)
     */
    private Long totalBookings;

    /**
     * Number of completed bookings
     */
    private Long completedBookings;

    /**
     * Number of cancelled bookings
     */
    private Long cancelledBookings;

    /**
     * Number of favorite barbers
     */
    private Integer favoriteBarbersCount;

    /**
     * Number of reviews left
     */
    private Integer reviewsCount;

    /**
     * Total amount spent
     */
    private Double totalSpent;

    // ===== PREFERENCES =====

    /**
     * Preferred language
     */
    private String preferredLanguage;

    /**
     * Notification preferences
     */
    private String notificationPreferences;

    /**
     * Marketing emails consent
     */
    private Boolean marketingConsent;

    /**
     * SMS notifications enabled
     */
    private Boolean smsNotifications;

    /**
     * Email notifications enabled
     */
    private Boolean emailNotifications;

    /**
     * Push notifications enabled
     */
    private Boolean pushNotifications;

    // ===== HELPER METHODS =====

    /**
     * Check if profile is complete
     */
    public boolean isProfileComplete() {
        return firstName != null && !firstName.isEmpty() &&
                lastName != null && !lastName.isEmpty() &&
                email != null && !email.isEmpty() &&
                phoneNumber != null && !phoneNumber.isEmpty() &&
                address != null && !address.isEmpty();
    }

    /**
     * Get completion percentage (0-100)
     */
    public int getProfileCompletionPercentage() {
        int total = 0;
        int completed = 0;

        // Required fields (60% weight)
        String[] requiredFields = { firstName, lastName, email, phoneNumber };
        for (String field : requiredFields) {
            total += 15;
            if (field != null && !field.isEmpty()) {
                completed += 15;
            }
        }

        // Optional fields (40% weight)
        String[] optionalFields = { address, city, profilePictureUrl, preferredLanguage };
        for (String field : optionalFields) {
            total += 13;
            if (field != null && !field.isEmpty()) {
                completed += 13;
            }
        }

        // Verification (extra points)
        if (emailVerified != null && emailVerified)
            completed += 1;
        total += 1;

        return total > 0 ? (completed * 100) / total : 0;
    }

    /**
     * Check if account needs verification
     */
    public boolean needsVerification() {
        return (emailVerified == null || !emailVerified) ||
                (phoneVerified == null || !phoneVerified);
    }
}