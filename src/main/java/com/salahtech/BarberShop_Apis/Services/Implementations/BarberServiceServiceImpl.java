package com.salahtech.BarberShop_Apis.Services.Implementations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.BarberService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.salahtech.BarberShop_Apis.Dtos.ServiceDto;
import com.salahtech.BarberShop_Apis.Dtos.ServiceStatsDto;
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

    @Override
    public ServiceDto create(Long barberId, ServiceDto dto, String imageUrl) {
        // 1. Vérifier le barbier
        Barber barber = barberRepo.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barbier introuvable"));

        // 2. Créer l'entité (Mapping DTO -> Entity)
        // Attention : BarberService est le nom de votre entité (pas le service Spring)
        com.salahtech.BarberShop_Apis.models.BarberService serviceEntity = new com.salahtech.BarberShop_Apis.models.BarberService();

        serviceEntity.setName(dto.getName());
        serviceEntity.setDescription(dto.getDescription());
        serviceEntity.setPrice(dto.getPrice());
        serviceEntity.setDuration(dto.getDuration()); // en minutes
        serviceEntity.setBarber(barber);

        // ✅ 3. L'ajout de l'image
        if (imageUrl != null) {
            serviceEntity.setImageUrl(imageUrl);
        }

        // 4. Sauvegarde
        var saved = serviceRepository.save(serviceEntity);

        // 5. Retour DTO
        return mapToDto(saved);
    }

    // Helper mapping
    private ServiceDto mapToDto(com.salahtech.BarberShop_Apis.models.BarberService s) {
        return ServiceDto.builder()
                .id(s.getId())
                .name(s.getName())
                .price(s.getPrice())
                .duration(s.getDuration())
                .imageUrl(s.getImageUrl()) // ✅ On renvoie l'URL au front
                .build();
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

    // ===== NOUVELLES MÉTHODES =====

    /**
     * Enrichit un ServiceDto avec ses statistiques
     */
    private ServiceDto enrichWithStats(ServiceDto dto) {
        if (dto == null || dto.getId() == null) {
            return dto;
        }

        try {
            // Récupération des stats depuis la base
            Long bookingsCount = serviceRepository.countBookingsByServiceId(dto.getId());
            BigDecimal revenue = serviceRepository.calculateServiceRevenue(dto.getId());
            Double rating = serviceRepository.calculateServiceRating(dto.getId());

            // Enrichissement du DTO
            dto.setBookingsCount(bookingsCount != null ? bookingsCount : 0L);
            dto.setRevenue(revenue != null ? revenue : BigDecimal.ZERO);
            dto.setRating(rating != null ? rating : 0.0);

        } catch (Exception e) {
            // Log l'erreur mais ne fait pas échouer la requête
            System.err
                    .println("Erreur lors du calcul des stats pour le service " + dto.getId() + ": " + e.getMessage());
        }

        return dto;
    }

    /**
     * Override de findById pour inclure les stats
     */
    // @Override
    // @Transactional(readOnly = true)
    // public ServiceDto findById(Long id) {
    // BarberService service = serviceRepository.findById(id)
    // .orElseThrow(() -> new RuntimeException("Service non trouvé avec l'ID: " +
    // id));
    // ServiceDto dto = ServiceDto.fromService(service);
    // return enrichWithStats(dto);
    // }

    /**
     * Override de search pour inclure les stats sur chaque service
     */
    // @Override
    // @Transactional(readOnly = true)
    // public Page<ServiceDto> search(String q, String category, Boolean active,
    // Long barberId, Pageable pageable) {
    // // Ta logique de recherche existante...
    // // Puis enrichir chaque résultat
    // Page<BarberService> services = /* ta requête existante */;
    // return services.map(s -> enrichWithStats(ServiceDto.fromService(s)));
    // }

    /**
     * Toggle le statut actif/inactif d'un service
     */
    @Override
    @Transactional
    public ServiceDto toggleServiceStatus(Long id, Boolean active) {
        BarberService service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service non trouvé avec l'ID: " + id));

        service.setActive(active);
        service.setUpdatedAt(LocalDateTime.now());

        BarberService updated = serviceRepository.save(service);
        return enrichWithStats(ServiceDto.fromService(updated));
    }

    /**
     * Duplique un service existant
     */
    @Override
    @Transactional
    public ServiceDto duplicateService(Long id) {
        BarberService original = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service non trouvé avec l'ID: " + id));

        // Création de la copie
        BarberService duplicate = new BarberService();
        duplicate.setName(original.getName() + " (Copie)");
        duplicate.setDescription(original.getDescription());
        duplicate.setPrice(original.getPrice());
        duplicate.setDuration(original.getDuration());
        duplicate.setCategory(original.getCategory());
        duplicate.setActive(true); // Actif par défaut
        duplicate.setImageUrl(original.getImageUrl());
        duplicate.setBarber(original.getBarber());
        duplicate.setCreatedAt(LocalDateTime.now());
        duplicate.setUpdatedAt(LocalDateTime.now());

        BarberService saved = serviceRepository.save(duplicate);
        return enrichWithStats(ServiceDto.fromService(saved));
    }

    /**
     * Récupère les statistiques détaillées d'un service
     */
    @Override
    @Transactional(readOnly = true)
    public ServiceStatsDto getServiceStats(Long id) {
        BarberService service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service non trouvé avec l'ID: " + id));

        // Stats globales
        Long bookingsCount = serviceRepository.countBookingsByServiceId(id);
        BigDecimal revenue = serviceRepository.calculateServiceRevenue(id);
        Double rating = serviceRepository.calculateServiceRating(id);

        // Calculs de tendances (optionnel)
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);
        YearMonth lastMonth = currentMonth.minusMonths(1);

        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        LocalDateTime startOfLastMonth = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfLastMonth = lastMonth.atEndOfMonth().atTime(23, 59, 59);

        // TODO: Ajouter des méthodes dans BookingRepository pour ces calculs
        // Pour l'instant, on retourne null
        Integer bookingsThisMonth = null;
        Integer bookingsLastMonth = null;
        BigDecimal revenueThisMonth = null;
        BigDecimal revenueLastMonth = null;

        return ServiceStatsDto.builder()
                .serviceId(id)
                .serviceName(service.getName())
                .bookingsCount(bookingsCount != null ? bookingsCount : 0L)
                .revenue(revenue != null ? revenue : BigDecimal.ZERO)
                .rating(rating != null ? rating : 0.0)
                .lastBooking(null) // TODO: Ajouter une méthode pour récupérer la dernière réservation
                .bookingsThisMonth(bookingsThisMonth)
                .bookingsLastMonth(bookingsLastMonth)
                .revenueThisMonth(revenueThisMonth)
                .revenueLastMonth(revenueLastMonth)
                .build();
    }
}
