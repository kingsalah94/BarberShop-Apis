package com.salahtech.BarberShop_Apis.security.config;

import java.io.IOException;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ApplicationUserService;
import com.salahtech.BarberShop_Apis.Services.RefreshTokenService;
import com.salahtech.BarberShop_Apis.Utils.JwtUtil;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final ApplicationUserService userService;
    private final RefreshTokenService refreshTokenService;

    public OAuth2AuthenticationSuccessHandler(JwtUtil jwtUtil,
                                              ApplicationUserService userService,
                                              RefreshTokenService refreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String providerId = (String) attributes.get("sub"); // Google
        if (providerId == null) providerId = (String) attributes.get("id"); // Facebook

        // Déterminer le provider (à adapter selon ton flow)
        String registrationId = request.getParameter("registration_id");
        AuthProvider provider = "facebook".equals(registrationId) ? AuthProvider.FACEBOOK : AuthProvider.GOOGLE;

        ApplicationUser user = userService.findByEmailOrCreate(email, name, provider, providerId);

        // Tokens
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        refreshTokenService.saveRefreshToken(user, refreshToken);

        // Redirection Front
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:4200/auth/callback")
                .queryParam("token", accessToken)
                .queryParam("refresh", refreshToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
