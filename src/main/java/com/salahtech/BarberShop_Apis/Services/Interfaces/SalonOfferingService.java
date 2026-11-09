package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.SalonServiceDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonServiceRequest;

import java.util.List;

/** Catalogue de services au niveau salon. */
public interface SalonOfferingService {
    List<SalonServiceDto> list(Long salonId, boolean activeOnly);
    SalonServiceDto create(Long salonId, SalonServiceRequest req);
    SalonServiceDto update(Long salonId, Long serviceId, SalonServiceRequest req);
    SalonServiceDto setActive(Long salonId, Long serviceId, boolean active);
    void delete(Long salonId, Long serviceId);
}
