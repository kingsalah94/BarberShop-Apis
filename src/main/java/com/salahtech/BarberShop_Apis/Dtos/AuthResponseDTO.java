package com.salahtech.BarberShop_Apis.Dtos;

import java.time.LocalDateTime;

import com.salahtech.BarberShop_Apis.models.ApplicationUser;

public class AuthResponseDTO {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private ApplicationUserDto user;
    private LocalDateTime issuedAt;
    
    // Constructors
    public AuthResponseDTO() {}
    
    public AuthResponseDTO(String accessToken, String refreshToken, Long expiresIn, ApplicationUserDto user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
        this.issuedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    
    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
    
    public ApplicationUserDto getUser() { return user; }
    public void setUser(ApplicationUserDto user) { this.user = user; }
    
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
}
