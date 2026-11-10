package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.SalonCatalogDetailDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonCatalogDto;

import java.util.List;

/** Catalogue public (côté client) : parcourir les salons, leurs services et horaires. */
public interface CatalogService {
    List<SalonCatalogDto> listSalons();
    SalonCatalogDetailDto getSalon(Long salonId);
    /** Derniers avis publics d'un salon (prénom + initiale du client). */
    List<com.salahtech.BarberShop_Apis.Dtos.SalonPublicReviewDto> latestReviews(Long salonId, int limit);
}
