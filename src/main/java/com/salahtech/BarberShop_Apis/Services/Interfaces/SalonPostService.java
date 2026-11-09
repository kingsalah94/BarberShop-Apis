package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.SalonPostDto;

import java.util.List;

/** Publications (réalisations) des salons. */
public interface SalonPostService {
    List<SalonPostDto> listForSalon(Long salonId);
    List<SalonPostDto> feed();
    SalonPostDto create(Long salonId, String imageUrl, String caption);
    void delete(Long salonId, Long postId);
}
