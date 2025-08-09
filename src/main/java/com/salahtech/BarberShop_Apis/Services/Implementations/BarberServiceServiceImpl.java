package com.salahtech.BarberShop_Apis.Services.Implementations;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.salahtech.BarberShop_Apis.models.BarberService;
import org.springframework.stereotype.Component;

import com.salahtech.BarberShop_Apis.Dtos.ServiceDto;
import com.salahtech.BarberShop_Apis.Enums.ErrorCodes;
import com.salahtech.BarberShop_Apis.Enums.ServiceCategory;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ServiceService;
import com.salahtech.BarberShop_Apis.reppsitories.BarberServiceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BarberServiceServiceImpl implements ServiceService {

    private final BarberServiceRepository serviceRepository;

    @Override
    public ServiceDto save(ServiceDto dto) {
        BarberService entity = ServiceDto.toEntity(dto);
        return ServiceDto.fromService(serviceRepository.save(entity));
    }

    @Override
    public ServiceDto findById(Long id) {
        return serviceRepository.findById(id)
                .map(ServiceDto::fromService)
                .orElseThrow(() -> new InvalideEntityException(
                        "Aucun service trouvé avec l'ID " + id,
                        ErrorCodes.SERVICE_NOT_FOUND
                ));
    }

    @Override
    public List<ServiceDto> findAll() {
        return serviceRepository.findAll().stream()
                .map(ServiceDto::fromService)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            log.error("ID du service à supprimer est null.");
            return;
        }
        serviceRepository.deleteById(id);
    }

    @Override
    public List<ServiceDto> findByBarberId(Long barberId) {
        return serviceRepository.findByBarberId(barberId).stream()
                .map(ServiceDto::fromService)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceDto> findByCategory(ServiceCategory category) {
        return serviceRepository.findByCategory(category).stream()
                .map(ServiceDto::fromService)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceDto> findByActive(Boolean active) {
        return serviceRepository.findByActive(active).stream()
                .map(ServiceDto::fromService)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceDto> findActiveServicesByBarberId(Long barberId) {
        return serviceRepository.findActiveServicesByBarberId(barberId).stream()
                .map(ServiceDto::fromService)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceDto> findActiveByCategoryOrderByPriceAsc(ServiceCategory category) {
        return serviceRepository.findActiveByCategoryOrderByPriceAsc(category).stream()
                .map(ServiceDto::fromService)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceDto> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return serviceRepository.findByPriceRangeAndActive(minPrice, maxPrice).stream()
                .map(ServiceDto::fromService)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceDto> findByMaxDuration(Integer maxDuration) {
        return serviceRepository.findByMaxDurationAndActive(maxDuration).stream()
                .map(ServiceDto::fromService)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceDto> findByNameContaining(String name) {
        return serviceRepository.findByNameContainingAndActive(name).stream()
                .map(ServiceDto::fromService)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceCategory> getDistinctActiveCategories() {
        return serviceRepository.findDistinctActiveCategories();
    }
}
