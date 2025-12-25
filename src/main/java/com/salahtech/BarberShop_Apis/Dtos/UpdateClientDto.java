package com.salahtech.BarberShop_Apis.Dtos;

import java.time.LocalDateTime;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for updating client profile information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateClientDto {

    /**
     * First name
     */
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    /**
     * Last name
     */
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    /**
     * Email address
     */
    @Email(message = "Invalid email format")
    private String email;

    /**
     * Phone number
     */
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    /**
     * Profile photo URL
     */
    private String ProfilePicture;

    private LocalDateTime birthDate;
    private String preferences;

    /**
     * Street address
     */
    @Size(max = 200, message = "Address must not exceed 200 characters")
    private String address;

    /**
     * City
     */
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    /**
     * Postal code
     */
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    /**
     * Country
     */
    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    /**
     * Preferred language (e.g., "en", "fr")
     */
    @Size(max = 5, message = "Language code must not exceed 5 characters")
    private String preferredLanguage;

    /**
     * Notification preferences (JSON or comma-separated values)
     * Example: "email,sms,push"
     */
    private String notificationPreferences;

    /**
     * Marketing emails consent
     */
    private Boolean marketingConsent;
}
