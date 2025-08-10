package com.salahtech.BarberShop_Apis.Services.Interfaces;


import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.salahtech.BarberShop_Apis.Dtos.BarberDto;

public interface BarberService {

    BarberDto save(BarberDto dto);

    BarberDto findById(Long id);

    BarberDto findByUserId(Long userId);

    List<BarberDto> findAll();

    List<BarberDto> findAllAvailable();

    List<BarberDto> findByLocation(String location, Pageable pageable);

    List<BarberDto> findBySalonName(String salonName, Pageable pageable);

    List<BarberDto> findByPriceRange(BigDecimal min, BigDecimal max, Pageable pageable);

    List<BarberDto> findByMinRating(BigDecimal minRating, Pageable pageable);

    List<BarberDto> findAvailableByLocation(String location, Pageable pageable);

    void delete(Long id);
}

