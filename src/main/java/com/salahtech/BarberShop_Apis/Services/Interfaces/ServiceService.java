package com.salahtech.BarberShop_Apis.Services.Interfaces;

import java.math.BigDecimal;
import java.util.List;

import com.salahtech.BarberShop_Apis.Dtos.ServiceDto;
import com.salahtech.BarberShop_Apis.Enums.ServiceCategory;

public interface ServiceService {

    ServiceDto save(ServiceDto dto);

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
}
