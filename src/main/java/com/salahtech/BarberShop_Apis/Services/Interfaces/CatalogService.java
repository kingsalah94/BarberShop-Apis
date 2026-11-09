package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.SalonCatalogDetailDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonCatalogDto;

import java.util.List;

/** Catalogue public (côté client) : parcourir les salons, leurs services et horaires. */
public interface CatalogService {
    List<SalonCatalogDto> listSalons();
    SalonCatalogDetailDto getSalon(Long salonId);
}
