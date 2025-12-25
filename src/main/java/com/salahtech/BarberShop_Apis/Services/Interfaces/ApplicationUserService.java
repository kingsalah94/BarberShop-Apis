package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.ApplicationUserDto;
import com.salahtech.BarberShop_Apis.Dtos.AuthRequestDTO;
import com.salahtech.BarberShop_Apis.Dtos.AuthResponseDTO;
import com.salahtech.BarberShop_Apis.Dtos.ChangePasswordRequest;
import com.salahtech.BarberShop_Apis.Dtos.RegisterRequestDTO;
import com.salahtech.BarberShop_Apis.Dtos.UpdateProfileRequest;
import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

public interface ApplicationUserService {

    AuthResponseDTO login(AuthRequestDTO authRequest);

    AuthResponseDTO register(RegisterRequestDTO req);

    AuthResponseDTO refreshToken(String refreshToken);

    void logout(String refreshToken);

    ApplicationUserDto updateMe(String email, UpdateProfileRequest req);

    ApplicationUserDto updateAvatar(Long userId, String fileUrl);

    void changePassword(ChangePasswordRequest request);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);

    void saveFcmToken(String email, String token);

    void verifyEmail(String token);

    ApplicationUser registerSystemAdmin(ApplicationUser user);

    ApplicationUser findById(Long id);

    ApplicationUser findByEmail(String email);

    ApplicationUser findByEmailOrCreate(String email, String name, AuthProvider provider, String providerId);

    // ApplicationUser findByEmailOrCreate(String email, String name, AuthProvider
    // provider, String providerId);

    // ApplicationUser findById(Long id);

    // ApplicationUser findByEmail(String email);

    // AuthResponseDTO login(AuthRequestDTO authRequest);

    // // AuthResponseDTO loginWithProvider(AuthRequestDTO authRequest, AuthProvider
    // // provider);

    // ApplicationUserDto updateMe(String email, UpdateProfileRequest req);

    // AuthResponseDTO register(RegisterRequestDTO registerRequest);

    // AuthResponseDTO refreshToken(String refreshToken);

    // void logout(String refreshToken);

    // void forgotPassword(String email);

    // void resetPassword(String token, String newPassword);

    // void verifyEmail(String token);

    // void saveRefreshToken(ApplicationUser user, String tokenValue);

    // void updatePassword(ApplicationUser user, String newPassword);

    // /**
    // * NOUVELLE MÉTHODE: Enregistre un utilisateur système
    // * Utilisé pour l'initialisation (admin, utilisateurs de test)
    // */
    // ApplicationUser registerSystemAdmin(ApplicationUser user);

    // void saveFcmToken(String email, String token);

    // ApplicationUserDto updateAvatar(Long userId, String imageUrl);
}
