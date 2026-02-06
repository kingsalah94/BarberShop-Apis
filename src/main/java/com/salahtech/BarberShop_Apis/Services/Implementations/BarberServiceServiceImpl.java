package com.salahtech.BarberShop_Apis.Services.Implementations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.BarberService;
import com.salahtech.BarberShop_Apis.models.Category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.salahtech.BarberShop_Apis.AOP.LogActivity;
import com.salahtech.BarberShop_Apis.Dtos.ServiceDto;
import com.salahtech.BarberShop_Apis.Dtos.ServiceStatsDto;
import com.salahtech.BarberShop_Apis.Enums.ErrorCodes;
import com.salahtech.BarberShop_Apis.Enums.ServiceCategory;
import com.salahtech.BarberShop_Apis.Exceptions.AccessDeniedException;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ServiceService;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BarberRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BarberServiceRepository;
import com.salahtech.BarberShop_Apis.reppsitories.CategoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BarberServiceServiceImpl implements ServiceService {

    private final BarberServiceRepository serviceRepository;
    private final BarberRepository barberRepo;
    private final ApplicationUserRepository userRepo;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    @LogActivity(action = "CREATE_SERVICE", entityName = "Service")
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
    @Transactional
    @LogActivity(action = "CREATE_SERVICE", entityName = "Service")
    public ServiceDto create(ServiceDto dto, String imageUrl, ApplicationUser connectedUser) {

        Barber barber;

        // On vérifie d'abord si c'est un membre du staff admin
        boolean isAdmin = connectedUser.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN") || r.getName().equals("SALON_OWNER"));

        // =========================================================================
        // 🧠 LOGIQUE INTELLIGENTE CORRIGÉE
        // =========================================================================

        // 1. CAS ADMIN AVEC SÉLECTION : L'Admin a choisi un barbier spécifique (ID > 0)
        if (isAdmin && dto.getBarberId() != null && dto.getBarberId() > 0) {
            barber = barberRepo.findById(dto.getBarberId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Barbier cible introuvable (ID: " + dto.getBarberId() + ")"));
        }
        // 2. CAS BARBIER CONNECTÉ (ou Admin qui a oublié de sélectionner)
        else {
            // On cherche le profil barbier lié à l'utilisateur connecté
            // ⚠️ ATTENTION : On utilise findByUserId, pas findById !
            barber = barberRepo.findById(connectedUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            isAdmin
                                    ? "Veuillez sélectionner un barbier dans la liste (Votre compte Admin n'est pas un barbier)."
                                    : "Aucun profil Barbier trouvé pour cet utilisateur."));
        }

        // =========================================================================
        // 2. RÉCUPÉRATION DE LA CATÉGORIE (Dynamique BDD)
        // =========================================================================

        if (dto.getCategoryId() == null) {
            throw new IllegalArgumentException("La catégorie est obligatoire (categoryId manquant).");
        }

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Catégorie introuvable (ID: " + dto.getCategoryId() + ")"));
        // =========================================================================
        // 🏗️ CRÉATION DE L'ENTITÉ
        // =========================================================================

        com.salahtech.BarberShop_Apis.models.BarberService serviceEntity = new com.salahtech.BarberShop_Apis.models.BarberService();

        serviceEntity.setName(dto.getName());
        serviceEntity.setDescription(dto.getDescription());
        serviceEntity.setPrice(dto.getPrice());
        serviceEntity.setDuration(dto.getDuration());
        serviceEntity.setBarber(barber); // ✅ Le bon barbier est assigné
        serviceEntity.setActive(true);

        serviceEntity.setCategory(category);

        // // Catégorie
        // if (dto.getCategory() != null) {
        // serviceEntity.setCategory(dto.getCategory());
        // } else {
        // serviceEntity.setCategory(ServiceCategory.OTHER);
        // }

        // Image
        if (imageUrl != null) {
            serviceEntity.setImageUrl(imageUrl);
        }

        // Sauvegarde
        var saved = serviceRepository.save(serviceEntity);

        return mapToDto(saved);
    }

    @Override
    @LogActivity(action = "UPDATE_SERVICE", entityName = "Service")
    public ServiceDto update(Long serviceId, Long userId, ServiceDto dto, String newImageUrl) {

        // 1. Récupérer le service existant en base
        var serviceEntity = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service introuvable avec l'ID : " + serviceId));

        // 2. Récupérer l'utilisateur connecté pour vérifier ses rôles
        var currentUser = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));
        // 3. (Optionnel) Vérification de sécurité
        // Vérifier que l'utilisateur qui modifie est bien le propriétaire du service
        // (ou un Admin)
        Long ownerId = serviceEntity.getBarber().getUser().getId();
        if (!ownerId.equals(userId) && !isAdmin) {
            throw new AccessDeniedException("Vous n'avez pas le droit de modifier ce service");
        }

        // 4. Mise à jour des champs standards (Texte, Prix, Durée...)
        // serviceEntity.setName(dto.getName());
        // serviceEntity.setDescription(dto.getDescription());
        // serviceEntity.setPrice(dto.getPrice());
        // serviceEntity.setDuration(dto.getDuration());

        // // Important : Mettre à jour le statut Actif/Inactif
        // serviceEntity.setActive(dto.getActive());

        // // 4. Mise à jour de la Catégorie
        // if (dto.getCategory() != null) {
        // serviceEntity.setCategory(dto.getCategory());
        // }

        if (dto.getName() != null)
            serviceEntity.setName(dto.getName());
        if (dto.getDescription() != null)
            serviceEntity.setDescription(dto.getDescription());
        if (dto.getPrice() != null)
            serviceEntity.setPrice(dto.getPrice());
        if (dto.getDuration() != null)
            serviceEntity.setDuration(dto.getDuration());
        if (dto.getActive() != null)
            serviceEntity.setActive(dto.getActive());

        // =========================================================================
        // 🏗️ MISE À JOUR DE LA CATÉGORIE (Logic dynamique)
        // =========================================================================

        if (dto.getCategoryId() != null) {
            // On vérifie si la catégorie a changé pour éviter un appel BDD inutile
            if (!serviceEntity.getCategory().getId().equals(dto.getCategoryId())) {
                Category newCategory = categoryRepository.findById(dto.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Catégorie introuvable ID: " + dto.getCategoryId()));

                serviceEntity.setCategory(newCategory);
            }
        }

        // ✅ 5. LOGIQUE CRITIQUE POUR L'IMAGE
        // Si newImageUrl n'est pas null, cela signifie qu'un nouveau fichier a été
        // uploadé.
        // On remplace l'ancienne URL.
        if (newImageUrl != null && !newImageUrl.isEmpty()) {
            serviceEntity.setImageUrl(newImageUrl);
        }
        // SINON (si newImageUrl est null), on ne fait RIEN.
        // L'entité garde l'URL qu'elle avait déjà en base de données via le findById au
        // début.

        // 6. Sauvegarder les modifications
        var savedEntity = serviceRepository.save(serviceEntity);

        // 7. Retourner le DTO mis à jour
        return mapToDto(savedEntity);
    }

    // Helper mapping
    private ServiceDto mapToDto(com.salahtech.BarberShop_Apis.models.BarberService s) {
        ServiceDto.ServiceDtoBuilder builder = ServiceDto.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription()) // N'oublie pas la description si elle existe
                .price(s.getPrice())
                .duration(s.getDuration())
                .imageUrl(s.getImageUrl())
                .active(s.getActive()); // Important pour le front

        // ✅ MAPPING DE LA CATÉGORIE (Entité -> Champs DTO)
        if (s.getCategory() != null) {
            builder.categoryId(s.getCategory().getId())
                    .categoryName(s.getCategory().getName())
                    .categoryIcon(s.getCategory().getIcon());
        }

        return builder.build();
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
    @LogActivity(action = "DELETE_SERVICE", entityName = "Service")
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
    @LogActivity(action = "TOGGLE_SERVICE_STATUS", entityName = "Service")
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
    @LogActivity(action = "DUPLICATE_SERVICE", entityName = "Service")
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
