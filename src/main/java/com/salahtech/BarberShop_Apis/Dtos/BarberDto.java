package com.salahtech.BarberShop_Apis.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.salahtech.BarberShop_Apis.models.Barber;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Barber.
 * Used for API communication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarberDto {

    private Long id;
    private String name;        // "Ahmed Benali"

   
    //private Long userId;

    private ApplicationUserDto userDto;

    @NotNull(message = "Salon name is required")
    @Size(min = 2, max = 100, message = "Salon name must be between 2 and 100 characters")
    private String salonName;

    @NotNull(message = "Location is required")
    @Size(min = 2, max = 255, message = "Location must be between 2 and 255 characters")
    private String location;


    private BigDecimal latitude;

    private BigDecimal longitude;

     private Integer experience; // en années

    private BigDecimal rating;

    private Integer reviewsCount;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private String specialties; // JSON string

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal priceFrom;

    private Boolean available;

    private String workingHours;

    private Integer totalReviews;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Champs pour la réponse
    private String userFirstName;
    private String userLastName;
    private String userEmail;
    private String profilePicture;


    public static BarberDto fromEntity(Barber barber) {

        if (barber == null) {
            return null;
        }

        return BarberDto.builder()
                .id(barber.getId())
                //.userDto(ApplicationUserDto.fromEntity(barber.getUser())) // Assuming User has a constructor that accepts userId
                .salonName(barber.getSalonName())
                .location(barber.getLocation())
                .latitude(barber.getLatitude())
                .longitude(barber.getLongitude())
                .rating(barber.getRating())
                .reviewsCount(barber.getReviewsCount())
                .description(barber.getDescription())
                .specialties(barber.getSpecialties())
                .priceFrom(barber.getPriceFrom())
                .available(barber.getAvailable())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()) // Assuming updatedAt is set to current time
                .build();
    }

    public static Barber toEntity(BarberDto dto) {
        Barber barber = new Barber();
        barber.setId(dto.id);
        barber.setUser(ApplicationUserDto.toEntity(dto.getUserDto())); // Assuming User has a constructor that accepts userId
        barber.setSalonName(dto.salonName);
        barber.setLocation(dto.location);
        barber.setLatitude(dto.latitude);
        barber.setLongitude(dto.longitude);
        barber.setRating(dto.rating);
        barber.setReviewsCount(dto.reviewsCount);
        barber.setDescription(dto.description);
        barber.setSpecialties(dto.specialties);
        barber.setPriceFrom(dto.priceFrom);
        barber.setAvailable(dto.available);
        barber.setCreatedAt(LocalDateTime.now()); // Assuming createdAt is set to current time
        return barber;
    }
}