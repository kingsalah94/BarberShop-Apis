package com.salahtech.BarberShop_Apis.models.Auth;

import jakarta.validation.constraints.NotBlank;

public class ValidateTokenRequest {
     @NotBlank(message = "Le token est obligatoire")
    private String token;

    public ValidateTokenRequest() {}

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
// This class is used to validate a JWT token.
// It contains a single field `token` which is the JWT token to be validated.
// The `@NotBlank` annotation ensures that the token is not null or empty.