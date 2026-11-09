package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.SalonServiceDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonServiceRequest;
import com.salahtech.BarberShop_Apis.Exceptions.BusinessException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonOfferingService;
import com.salahtech.BarberShop_Apis.models.Category;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.models.SalonOffering;
import com.salahtech.BarberShop_Apis.repositories.CategoryRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonOfferingRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SalonOfferingServiceImpl implements SalonOfferingService {

    private final SalonOfferingRepository offeringRepository;
    private final SalonRepository salonRepository;
    private final CategoryRepository categoryRepository;

    private Salon salon(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable: " + salonId));
    }

    private SalonOffering owned(Long salonId, Long serviceId) {
        SalonOffering o = offeringRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service introuvable: " + serviceId));
        if (o.getSalon() == null || !o.getSalon().getId().equals(salonId)) {
            throw new BusinessException("Ce service n'appartient pas au salon " + salonId);
        }
        return o;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalonServiceDto> list(Long salonId, boolean activeOnly) {
        salon(salonId);
        List<SalonOffering> rows = activeOnly
                ? offeringRepository.findBySalon_IdAndActiveTrueOrderByNameAsc(salonId)
                : offeringRepository.findBySalon_IdOrderByNameAsc(salonId);
        return rows.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public SalonServiceDto create(Long salonId, SalonServiceRequest req) {
        Salon salon = salon(salonId);
        SalonOffering o = SalonOffering.builder()
                .salon(salon)
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .durationMinutes(req.getDurationMinutes())
                .imageUrl(req.getImageUrl())
                .category(resolveCategory(req.getCategoryId()))
                .active(req.getActive() == null || req.getActive())
                .build();
        SalonOffering saved = offeringRepository.save(o);
        log.info("Service salon créé id={} salon={} '{}'", saved.getId(), salonId, saved.getName());
        return toDto(saved);
    }

    @Override
    public SalonServiceDto update(Long salonId, Long serviceId, SalonServiceRequest req) {
        SalonOffering o = owned(salonId, serviceId);
        if (req.getName() != null) o.setName(req.getName());
        if (req.getDescription() != null) o.setDescription(req.getDescription());
        if (req.getPrice() != null) o.setPrice(req.getPrice());
        if (req.getDurationMinutes() != null) o.setDurationMinutes(req.getDurationMinutes());
        if (req.getImageUrl() != null) o.setImageUrl(req.getImageUrl());
        if (req.getCategoryId() != null) o.setCategory(resolveCategory(req.getCategoryId()));
        if (req.getActive() != null) o.setActive(req.getActive());
        return toDto(offeringRepository.save(o));
    }

    @Override
    public SalonServiceDto setActive(Long salonId, Long serviceId, boolean active) {
        SalonOffering o = owned(salonId, serviceId);
        o.setActive(active);
        return toDto(offeringRepository.save(o));
    }

    @Override
    public void delete(Long salonId, Long serviceId) {
        offeringRepository.delete(owned(salonId, serviceId));
    }

    // ----- helpers -----

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable: " + categoryId));
    }

    private SalonServiceDto toDto(SalonOffering o) {
        return SalonServiceDto.builder()
                .id(o.getId())
                .salonId(o.getSalon() != null ? o.getSalon().getId() : null)
                .name(o.getName())
                .description(o.getDescription())
                .price(o.getPrice())
                .durationMinutes(o.getDurationMinutes())
                .imageUrl(o.getImageUrl())
                .categoryId(o.getCategory() != null ? o.getCategory().getId() : null)
                .categoryName(o.getCategory() != null ? o.getCategory().getName() : null)
                .active(o.isActive())
                .build();
    }
}
