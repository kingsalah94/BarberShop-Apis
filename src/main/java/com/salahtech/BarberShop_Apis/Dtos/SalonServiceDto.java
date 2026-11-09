package com.salahtech.BarberShop_Apis.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Service du catalogue d'un salon. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonServiceDto {
    private Long id;
    private Long salonId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationMinutes;
    private String imageUrl;
    private Long categoryId;
    private String categoryName;
    private boolean active;
}
