package com.salahtech.BarberShop_Apis.Dtos;

import java.math.BigDecimal;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

// DTO pour la création complète
@Data
public class CreateUserWithProfileRequest {
    // Infos utilisateur
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String password;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String phone;
    @NotNull
    private ApplicationUserType userType;

    // Infos Client (si type = CLIENT)
    private ClientProfileData clientData;

    // Infos Barber (si type = BARBER)
    private BarberProfileData barberData;

    @Data
    public static class ClientProfileData {
        private String preferences;
        private String notes;
    }

    @Data
    public static class BarberProfileData {
        @NotBlank
        private String salonName;
        @NotBlank
        private String location;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String description;
        private String specialties;
        @NotNull
        private BigDecimal priceFrom;
        private String workingHours;
    }
}
