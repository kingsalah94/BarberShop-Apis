package com.salahtech.BarberShop_Apis.Dtos;

import java.math.BigDecimal;

import com.salahtech.BarberShop_Apis.Enums.ServiceCategory;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceSearchDto {

    private String name;
    private ServiceCategory category;
    
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    
    private Integer maxDuration;
    
    @Builder.Default
    private Boolean activeOnly = true;
    
    private Long barberId;
    
    // Paramètres de tri
    private String sortBy; // name, price, duration, category
    private String sortDirection; // asc, desc
}
