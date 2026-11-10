package com.salahtech.BarberShop_Apis.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Carte de fidélité d'un client dans un salon. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyStatusDto {
    private Long salonId;
    private String salonName;
    /** Taille de la carte (visites payées pour une prestation offerte). */
    private int cardSize;
    /** Visites payées (les prestations offertes ne comptent pas). */
    private long paidVisits;
    /** Tampons sur la carte en cours (0..cardSize-1). */
    private long stamps;
    private long rewardsEarned;
    private long rewardsUsed;
    private long rewardsAvailable;
}
