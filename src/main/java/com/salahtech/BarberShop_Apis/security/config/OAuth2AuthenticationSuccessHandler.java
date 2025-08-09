package com.salahtech.BarberShop_Apis.security.config;

import java.io.IOException;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.Services.RefreshTokenService;
import com.salahtech.BarberShop_Apis.Services.Implementations.ApplicationUserServiceImpl;
import com.salahtech.BarberShop_Apis.Utils.JwtUtil;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@AllArgsConstructor
@NoArgsConstructor
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private ApplicationUserServiceImpl userService;

    @Autowired
    private RefreshTokenService refreshTokenService;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String providerId = (String) attributes.get("sub"); // Google
        if (providerId == null) {
            providerId = (String) attributes.get("id"); // Facebook
        }
        
        // Déterminer le provider
        String registrationId = request.getParameter("registration_id");
        AuthProvider provider = AuthProvider.GOOGLE;
        if ("facebook".equals(registrationId)) {
            provider = AuthProvider.FACEBOOK;
        }
        
        ApplicationUser user = userService.findByEmailOrCreate(email, name, provider, providerId);
        
        // Générer les tokens
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        
        // Sauvegarder le refresh token
       refreshTokenService.saveRefreshToken(user, refreshToken);

        
        // Rediriger vers le frontend avec les tokens
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:4200/auth/callback")
                .queryParam("token", accessToken)
                .queryParam("refresh", refreshToken)
                .build().toUriString();
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
