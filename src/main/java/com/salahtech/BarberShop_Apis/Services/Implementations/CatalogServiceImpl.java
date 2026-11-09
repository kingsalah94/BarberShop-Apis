package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.SalonCatalogDetailDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonCatalogDto;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.CatalogService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonHoursService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonOfferingService;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.repositories.SalonOfferingRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final SalonRepository salonRepository;
    private final SalonOfferingRepository offeringRepository;
    private final SalonReviewRepository reviewRepository;
    private final SalonOfferingService offeringService;
    private final SalonHoursService hoursService;

    /** Note moyenne arrondie à 1 décimale (null si aucun avis). */
    private Double ratingOf(Long salonId) {
        Double avg = reviewRepository.avgRating(salonId);
        return avg == null ? null : Math.round(avg * 10) / 10.0;
    }

    @Override
    public List<SalonCatalogDto> listSalons() {
        return salonRepository.findAll().stream()
                .map(s -> SalonCatalogDto.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .address(s.getAddress())
                        .phoneNumber(s.getPhoneNumber())
                        .latitude(s.getLatitude())
                        .longitude(s.getLongitude())
                        .establishmentTypeName(s.getType() != null ? s.getType().getName() : null)
                        .servicesCount(offeringRepository.countBySalon_IdAndActiveTrue(s.getId()))
                        .rating(ratingOf(s.getId()))
                        .reviewsCount(reviewRepository.countBySalon_Id(s.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public SalonCatalogDetailDto getSalon(Long salonId) {
        Salon s = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable: " + salonId));
        return SalonCatalogDetailDto.builder()
                .id(s.getId())
                .name(s.getName())
                .address(s.getAddress())
                .phoneNumber(s.getPhoneNumber())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .establishmentTypeName(s.getType() != null ? s.getType().getName() : null)
                .rating(ratingOf(salonId))
                .reviewsCount(reviewRepository.countBySalon_Id(salonId))
                .services(offeringService.list(salonId, true)) // services actifs
                .hours(hoursService.getWeeklyHours(salonId))
                .build();
    }
}
