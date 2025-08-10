package com.salahtech.BarberShop_Apis.Services.Implementations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.salahtech.BarberShop_Apis.Dtos.BarberDto;
import com.salahtech.BarberShop_Apis.Enums.ErrorCodes;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.BarberService;
import com.salahtech.BarberShop_Apis.Validators.BarberValidator;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BarberRepository;

import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BarberServiceImpl implements BarberService {

    private final BarberRepository barberRepository;

    ApplicationUserRepository userRepository;

    @Override
    public BarberDto save(BarberDto dto) {
        List<String> errors = BarberValidator.validate(dto);
        if (!errors.isEmpty()) {
            log.error("Barber is not valid: {}", errors);
            throw new InvalideEntityException("Barber is not valid", ErrorCodes.BARBER_NOT_VALID, errors);
        }

        log.info("Saving new barber: {}", dto.getSalonName());
        Barber saved = barberRepository.save(BarberDto.toEntity(dto));
        return BarberDto.fromEntity(saved);
    }

    @Override
    public BarberDto findById(Long id) {
        if (id == null) {
            log.error("Barber ID is null");
            return null;
        }

        return barberRepository.findById(id)
                .map(BarberDto::fromEntity)
                .orElseThrow(() ->
                        new EntityNotFoundException("No barber found with ID " + id)
                );
    }

    @Override
    public BarberDto findByUserId(Long userId) {
        if (userId == null) {
            log.error("User ID is null when looking for barber");
            return null;
        }

        return barberRepository.findByUserId(userId)
                .map(BarberDto::fromEntity)
                .orElseThrow(() ->
                        new InvalideEntityException("No barber found with user ID " + userId, ErrorCodes.BARBER_NOT_FOUND)
                );
    }

    @Override
    public List<BarberDto> findAll() {
        return barberRepository.findAll().stream()
                .map(BarberDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BarberDto> findAllAvailable() {
        return barberRepository.findByAvailable(true).stream()
                .map(BarberDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BarberDto> findByLocation(String location,Pageable pageable) {
        return barberRepository.findByLocationContaining(location).stream()
                .map(BarberDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BarberDto> findBySalonName(String salonName,Pageable pageable) {
        return barberRepository.findBySalonNameContaining(salonName).stream()
                .map(BarberDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BarberDto> findByPriceRange(BigDecimal min, BigDecimal max, Pageable pageable) {
        return barberRepository.findByPriceRange(min, max).stream()
                .map(BarberDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BarberDto> findByMinRating(BigDecimal minRating, Pageable pageable) {
        return barberRepository.findByMinRatingOrderByRatingDesc(minRating).stream()
                .map(BarberDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BarberDto> findAvailableByLocation(String location, Pageable pageable) {
        return barberRepository.findAvailableByLocation(location).stream()
                .map(BarberDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            log.error("Barber ID is null for deletion");
            return;
        }

        log.warn("Deleting barber with ID: {}", id);
        barberRepository.deleteById(id);
    }



}
