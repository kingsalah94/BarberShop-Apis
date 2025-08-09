package com.salahtech.BarberShop_Apis.Dtos;

import lombok.Data;

import java.math.BigDecimal;

import com.salahtech.BarberShop_Apis.Enums.ServiceCategory;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BarberSearchDto {

    private String location;
    private Double latitude;
    private Double longitude;
    private Double radiusKm; // Rayon de recherche en kilomètres
    
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    
    private BigDecimal minRating;
    
    private ServiceCategory serviceCategory;
    
    private Boolean availableOnly = true;
    
    private String salonName;
    private String specialty;
    
    // Paramètres de tri
    private String sortBy; // rating, price, distance, name
    private String sortDirection; // asc, desc
}
