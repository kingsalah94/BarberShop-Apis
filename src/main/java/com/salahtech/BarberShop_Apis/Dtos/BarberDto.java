package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.models.Barber;
import lombok.*;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarberDto {

    private Long id;

    private Long userId; // on passe l’ID, pas l’entité
    private ApplicationUserDto userDto; // lecture seule si tu veux renvoyer info user

    @NotNull
    @Size(min = 2, max = 100)
    private String salonName;

    @NotNull
    @Size(min = 2, max = 255)
    private String location;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer experience;
    private BigDecimal rating;
    private Integer reviewsCount;

    @Size(max = 1000)
    private String description;

    private String specialties; // JSON
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal priceFrom;

    private Boolean available;
    private String workingHours;
    private Integer totalReviews;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Champs de confort
    private String userFirstName;
    private String userLastName;
    private String userEmail;
    private String profilePicture;

    public static BarberDto fromEntity(Barber b) {
        if (b == null)
            return null;
        var u = b.getUser();
        return BarberDto.builder()
                .id(b.getId())
                .salonName(b.getSalonName())
                .location(b.getLocation())
                .latitude(b.getLatitude())
                .longitude(b.getLongitude())
                .rating(b.getRating())
                .reviewsCount(b.getReviewsCount())
                .description(b.getDescription())
                .specialties(b.getSpecialties())
                .priceFrom(b.getPriceFrom())
                .available(b.getAvailable())
                .workingHours(b.getWorkingHours())
                .totalReviews(b.getTotalReviews())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .userId(u != null ? u.getId() : null)
                .userFirstName(u != null ? u.getFirstName() : null)
                .userLastName(u != null ? u.getLastName() : null)
                .userEmail(u != null ? u.getEmail() : null)
                .profilePicture(u != null ? u.getProfileImageUrl() : null)
                .build();
    }

    /**
     * ⚠️ Ne rattache pas user ici. Laisse le service charger l’ApplicationUser par
     * userId.
     */
    public static Barber toEntity(BarberDto dto) {
        if (dto == null)
            return null;
        Barber b = new Barber();
        b.setId(dto.getId());
        b.setSalonName(dto.getSalonName());
        b.setLocation(dto.getLocation());
        b.setLatitude(dto.getLatitude());
        b.setLongitude(dto.getLongitude());
        b.setRating(dto.getRating());
        b.setReviewsCount(dto.getReviewsCount());
        b.setDescription(dto.getDescription());
        b.setSpecialties(dto.getSpecialties());
        b.setPriceFrom(dto.getPriceFrom());
        b.setAvailable(dto.getAvailable());
        b.setWorkingHours(dto.getWorkingHours());
        // user + dates gérés par service/JPA
        return b;
    }
}
