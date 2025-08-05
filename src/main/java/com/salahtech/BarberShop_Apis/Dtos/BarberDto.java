package com.salahtech.BarberShop_Apis.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Barber;

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

    //private Long userId;

    private ApplicationUserDto userDto;

    private String salonName;

    private String location;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private BigDecimal rating;

    private Integer reviewsCount;

    private String description;

    private String specialties; // JSON string

    private BigDecimal priceFrom;

    private Boolean available;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;



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
