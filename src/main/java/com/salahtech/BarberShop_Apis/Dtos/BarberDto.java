package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.models.Barber;
import lombok.*;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarberDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;
    private ApplicationUserDto userDto;

    @NotNull
    @Size(min = 2, max = 100)
    private String salonName;

    @NotNull
    @Size(min = 2, max = 255)
    private String location;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private Double distance;

    private Integer experience;
    private BigDecimal rating;
    private Integer reviewsCount;

    @Size(max = 1000)
    private String description;

    private String specialties;
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

    // C'est l'image du Barbier (Salon)
    private String profilePicture;

    // Tenant : salon de rattachement du barbier (peut être null si non rattaché)
    private Long salonId;

    public static BarberDto fromEntity(Barber b) {
        if (b == null)
            return null;
        var u = b.getUser();

        return BarberDto.builder()
                .id(b.getId())
                .salonName(b.getSalonName())
                .location(b.getLocation())
                .latitude(b.getLatitude())
                .distance(b.getDistance())
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
                .experience(b.getExperience()) // N'oublie pas l'expérience

                // Info User
                .userId(u != null ? u.getId() : null)
                .userFirstName(u != null ? u.getFirstName() : null)
                .userLastName(u != null ? u.getLastName() : null)
                .userEmail(u != null ? u.getEmail() : null)

                // ✅ CORRECTION ICI : On prend l'image du Barbier, pas celle du User
                // Si le barbier n'a pas d'image, on peut fallback sur le user ou laisser null
                .profilePicture(b.getProfileImageUrl() != null ? b.getProfileImageUrl()
                        : (u != null ? u.getProfileImageUrl() : null))

                .salonId(b.getSalon() != null ? b.getSalon().getId() : null)

                .build();
    }

    public static Barber toEntity(BarberDto dto) {
        if (dto == null)
            return null;
        Barber b = new Barber();
        b.setId(dto.getId());
        b.setSalonName(dto.getSalonName());
        b.setLocation(dto.getLocation());
        b.setLatitude(dto.getLatitude());
        b.setDistance(dto.distance);
        b.setLongitude(dto.getLongitude());
        b.setRating(dto.getRating());
        b.setReviewsCount(dto.getReviewsCount());
        b.setDescription(dto.getDescription());
        b.setSpecialties(dto.getSpecialties());
        b.setPriceFrom(dto.getPriceFrom());
        b.setAvailable(dto.getAvailable());
        b.setWorkingHours(dto.getWorkingHours());
        b.setExperience(dto.getExperience());

        // Note: profileImageUrl est géré par le service via l'argument String, pas ici
        return b;
    }
}