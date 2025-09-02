package com.salahtech.BarberShop_Apis.Services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.salahtech.BarberShop_Apis.Utils.JwtUtil;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.RefreshToken;
import com.salahtech.BarberShop_Apis.reppsitories.RefreshTokenRepository;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public void saveRefreshToken(ApplicationUser user, String tokenValue) {
        refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);

        var now = LocalDateTime.now();
        var refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(now.plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000))
                .revoked(false)
                .createdAt(now) // <= important si pas de @CreationTimestamp
                .build();

        refreshTokenRepository.save(refreshToken);
    }
}
