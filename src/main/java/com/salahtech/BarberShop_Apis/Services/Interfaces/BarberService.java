package com.salahtech.BarberShop_Apis.Services.Interfaces;


import java.math.BigDecimal;
import java.util.List;

import com.salahtech.BarberShop_Apis.Dtos.BarberDto;

public interface BarberService {

    BarberDto save(BarberDto dto);

    BarberDto findById(Long id);

    BarberDto findByUserId(Long userId);

    List<BarberDto> findAll();

    List<BarberDto> findAllAvailable();

    List<BarberDto> findByLocation(String location);

    List<BarberDto> findBySalonName(String salonName);

    List<BarberDto> findByPriceRange(BigDecimal min, BigDecimal max);

    List<BarberDto> findByMinRating(BigDecimal minRating);

    List<BarberDto> findAvailableByLocation(String location);

    void delete(Long id);
}

