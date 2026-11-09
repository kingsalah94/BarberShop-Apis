package com.salahtech.BarberShop_Apis.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Prestataire réservable, vu par le CLIENT (catalogue public) : aucune donnée
 * de contact — prénom + initiale du nom et photo uniquement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonStaffDto {
    private Long id;
    private String displayName;
    private String photoUrl;
}
