package com.salahtech.BarberShop_Apis.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Référence légère à une localité (élément de chemin). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalityRefDto {
    private Long id;
    private String type;
    private String name;
}
