package com.salahtech.BarberShop_Apis.Services.Interfaces;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.salahtech.BarberShop_Apis.Dtos.ServiceDto;
import com.salahtech.BarberShop_Apis.Dtos.ServiceStatsDto;
import com.salahtech.BarberShop_Apis.Enums.ServiceCategory;

public interface ServiceService {

    ServiceDto save(ServiceDto dto);

    ServiceDto create(Long barberId, ServiceDto serviceDto, String imageUrl);

    ServiceDto findById(Long id);

    List<ServiceDto> findAll();

    void delete(Long id);

    List<ServiceDto> findByBarberId(Long barberId);

    List<ServiceDto> findByCategory(ServiceCategory category);

    List<ServiceDto> findByActive(Boolean active);

    List<ServiceDto> findActiveServicesByBarberId(Long barberId);

    List<ServiceDto> findActiveByCategoryOrderByPriceAsc(ServiceCategory category);

    List<ServiceDto> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);

    List<ServiceDto> findByMaxDuration(Integer maxDuration);

    List<ServiceDto> findByNameContaining(String name);

    List<ServiceCategory> getDistinctActiveCategories();

    Page<ServiceDto> search(String q, String category, Boolean active, Long barberId, Pageable pageable);

    /**
     * Toggle le statut actif/inactif d'un service
     */
    ServiceDto toggleServiceStatus(Long id, Boolean active);

    /**
     * Duplique un service existant
     */
    ServiceDto duplicateService(Long id);

    /**
     * Récupère les statistiques détaillées d'un service
     */
    ServiceStatsDto getServiceStats(Long id);
}
