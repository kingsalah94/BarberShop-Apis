package com.salahtech.BarberShop_Apis.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Statistiques agrégées d'une zone (descendants inclus). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalityStatDto {
    private Long id;
    private String name;
    private String type;
    private String parentName;
    private long salons;
    private long clients;
    private long bookings;
    private long bookings30d;
}
