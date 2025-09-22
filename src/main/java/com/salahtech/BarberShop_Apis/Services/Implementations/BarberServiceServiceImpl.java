package com.salahtech.BarberShop_Apis.Services.Implementations;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.salahtech.BarberShop_Apis.models.BarberService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.salahtech.BarberShop_Apis.Dtos.ServiceDto;
import com.salahtech.BarberShop_Apis.Enums.ErrorCodes;
import com.salahtech.BarberShop_Apis.Enums.ServiceCategory;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ServiceService;
import com.salahtech.BarberShop_Apis.reppsitories.BarberRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BarberServiceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BarberServiceServiceImpl implements ServiceService {

    private final BarberServiceRepository serviceRepository;
    private final BarberRepository barberRepo;

    @Override
    public ServiceDto save(ServiceDto dto) {
        var barber = barberRepo.findById(dto.getBarberId())
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found: " + dto.getBarberId()));

        // 2) mapper DTO -> entity (ton toEntity ne set pas barber)
        var entity = ServiceDto.toEntity(dto);
        // BarberService entity = ServiceDto.toEntity(dto);
        // 3) ATTACHER le barbier (clé étrangère barber_id)
        entity.setBarber(barber);
        var saved = serviceRepository.save(entity);
        return ServiceDto.fromService(saved);
    }

    // src/main/java/.../Services/Implementations/BarberServiceServiceImpl.java
    @Override
    @Transactional(readOnly = true)
    public Page<ServiceDto> search(String q, String category, Boolean active, Long barberId, Pageable pageable) {
        Specification<BarberService> spec = Specification.where(null);

        if (q != null && !q.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + q.toLowerCase() + "%")));
        }
        if (category != null && !category.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("category"), category));
        }
        if (active != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("active"), active));
        }
        if (barberId != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("barber").get("id"), barberId));
        }

        return serviceRepository.findAll(spec, pageable).map(ServiceDto::fromService);
    }

    @Override
    public ServiceDto findById(Long id) {
        return serviceRepository.findById(id)
                .map(ServiceDto::fromService)
                .orElseThrow(() -> new InvalideEntityException(
                        "Aucun service trouvé avec l'ID " + id,
                        ErrorCodes.SERVICE_NOT_FOUND));
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
