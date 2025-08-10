package com.salahtech.BarberShop_Apis.models;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

// RefreshTokenRequest
@Entity
@Data
@AllArgsConstructor
@Builder
@Table(name = "refresh_tokens", indexes = @Index(name="idx_token", columnList = "token", unique = true))
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String token;
    
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private ApplicationUser user;
    
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;
    
    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private Boolean revoked = false;
    // Constructors
    public RefreshToken() {}
    
    public RefreshToken(String token, ApplicationUser user, LocalDateTime expiryDate) {
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public ApplicationUser getUser() { return user; }
    public void setUser(ApplicationUser user) { this.user = user; }
    
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}
