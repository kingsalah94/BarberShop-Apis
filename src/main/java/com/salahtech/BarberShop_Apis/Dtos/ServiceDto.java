package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.ServiceCategory;
import com.salahtech.BarberShop_Apis.models.BarberService;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;

    @NotNull
    @Size(min = 2, max = 100)
    private String name;

    @NotNull
    private Long barberId;

    @Size(max = 500)
    private String description;

    private String imageUrl;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @NotNull
    @Min(1)
    private Integer duration;

    // @NotNull
    // private ServiceCategory category;

    @Builder.Default
    private Boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long categoryId; // ✅ AJOUTER pour l'écriture (création)
    private String categoryName; // ✅ AJOUTER pour la lecture (affichage)
    private String categoryIcon; // ✅ AJOUTER pour l'affichage mobile

    // Lecture
    private String barberName;
    private String salonName;

    // ===== NOUVELLES STATISTIQUES =====
    @Builder.Default
    private Long bookingsCount = 0L;

    @Builder.Default
    private BigDecimal revenue = BigDecimal.ZERO;

    @Builder.Default
    private Double rating = 0.0;

    /**
     * Retourne l'URL complète de l'image
     * Ex: http://localhost:8080/api/files/services/image123.jpg
     */
    public String getFullImageUrl() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        // Si c'est déjà une URL complète (commence par http), la retourner telle quelle
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }

        // Sinon, construire l'URL complète
        // TODO: Récupérer le baseUrl depuis la config (application.properties)
        String baseUrl = "http://localhost:8080";
        return baseUrl + "/api/files/services/" + imageUrl;
    }

    public static ServiceDto fromService(BarberService s) {
        if (s == null)
            return null;
        ServiceDto dto = ServiceDto.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .price(s.getPrice())
                .duration(s.getDuration())
                // .category(s.getCategory())
                .categoryId(s.getCategory().getId())
                .categoryName(s.getCategory().getName())
                .categoryIcon(s.getCategory().getIcon())
                .active(s.getActive())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .imageUrl(s.getImageUrl())
                // Stats initialisées par défaut (seront enrichies par le service)
                .bookingsCount(0L)
                .revenue(BigDecimal.ZERO)
                .rating(0.0)
                .build();

        // if (s.getBarber() != null) {
        // dto.setBarberId(s.getBarber().getId());
        // dto.setBarberName(s.getBarber().getName());
        // dto.setSalonName(s.getBarber().getSalonName());
        // }
        if (s.getCategory() != null) {
            dto.setCategoryId(s.getCategory().getId());
            dto.setCategoryName(s.getCategory().getName());
            dto.setCategoryIcon(s.getCategory().getIcon());
        }

        return dto;
    }

    /** DTO -> entity (sans lier le barber ici). */
    public static BarberService toEntity(ServiceDto dto) {
        if (dto == null)
            return null;
        BarberService s = new BarberService();
        s.setId(dto.getId());
        s.setName(dto.getName());
        s.setDescription(dto.getDescription());
        s.setPrice(dto.getPrice());
        s.setDuration(dto.getDuration());
        // s.setCategory(dto.getCategory());
        s.setActive(dto.getActive());
        s.setImageUrl(dto.getImageUrl());
        // barber + dates gérés par service/JPA
        return s;
    }
}