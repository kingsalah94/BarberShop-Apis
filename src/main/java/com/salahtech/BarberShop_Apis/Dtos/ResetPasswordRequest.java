package com.salahtech.BarberShop_Apis.Dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    private String token; // Le token reçu par email

    @NotBlank
    @Size(min = 6)
    private String newPassword;
}