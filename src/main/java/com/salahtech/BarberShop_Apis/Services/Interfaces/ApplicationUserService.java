package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.AuthRequestDTO;
import com.salahtech.BarberShop_Apis.Dtos.AuthResponseDTO;
import com.salahtech.BarberShop_Apis.Dtos.RegisterRequestDTO;
import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

public interface ApplicationUserService {

    ApplicationUser findByEmailOrCreate(String email, String name, AuthProvider provider, String providerId);

    ApplicationUser findById(Long id);

    ApplicationUser findByEmail(String email);

    AuthResponseDTO login(AuthRequestDTO authRequest);

    AuthResponseDTO register(RegisterRequestDTO registerRequest);

    AuthResponseDTO refreshToken(String refreshToken);

    void logout(String refreshToken);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);

    void verifyEmail(String token);

    void saveRefreshToken(ApplicationUser user, String tokenValue);

}
