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
        refreshTokenRepository.findByUser(user)
                .ifPresent(refreshTokenRepository::delete);

        RefreshToken refreshToken = new RefreshToken(
                tokenValue,
                user,
                LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000)
        );

        refreshTokenRepository.save(refreshToken);
    }
}
