package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.models.Post;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BarberFullProfileDto {

    // --- 1. INFO DE BASE ---
    private Long id;
    private String salonName;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address; // ou location
    private String avatarUrl;
    private String bio;

    // --- 2. SOCIAL PROOF ---
    private BigDecimal rating;
    private Integer reviewCount;
    private Boolean isFavorite; // Spécifique à l'utilisateur qui regarde

    // --- 3. CONTENU ---
    private List<ServiceDto> services; // Ses prestations (Prix, Durée)
    private List<Post> portfolio; // Ses meilleures photos
    private List<ReviewDto> recentReviews; // Les 3 ou 5 derniers avis

    // --- 4. DISPO (Optionnel) ---
    private Boolean isOpenNow; // Calculé en temps réel
}