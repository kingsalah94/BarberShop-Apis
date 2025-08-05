package com.salahtech.BarberShop_Apis.models.Auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequest {


    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    public ForgotPasswordRequest() {}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
// This class is used to handle requests for forgotten passwords.
// It contains a single field `email` which is the email address of the user.
// The `@NotBlank` annotation ensures that the email is not null or empty,
// and the `@Email` annotation ensures that the email is in a valid format.
