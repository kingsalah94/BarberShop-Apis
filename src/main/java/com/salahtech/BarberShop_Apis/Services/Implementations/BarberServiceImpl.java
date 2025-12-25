package com.salahtech.BarberShop_Apis.Services.Implementations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.salahtech.BarberShop_Apis.Dtos.BarberDto;
import com.salahtech.BarberShop_Apis.Dtos.BarberFullProfileDto;
import com.salahtech.BarberShop_Apis.Dtos.ReviewDto;
import com.salahtech.BarberShop_Apis.Dtos.ServiceDto;
import com.salahtech.BarberShop_Apis.Enums.ErrorCodes;
import com.salahtech.BarberShop_Apis.Exceptions.ConflictException;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.AvailabilityService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.BarberServices;
import com.salahtech.BarberShop_Apis.Validators.BarberValidator;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.BarberService;
import com.salahtech.BarberShop_Apis.models.Post;
import com.salahtech.BarberShop_Apis.models.Review;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BarberRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BarberServiceRepository;
import com.salahtech.BarberShop_Apis.reppsitories.ClientFavoriteRepository;
import com.salahtech.BarberShop_Apis.reppsitories.PostRepository;
import com.salahtech.BarberShop_Apis.reppsitories.ReviewRepository;

import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BarberServiceImpl implements BarberServices {

    private final BarberRepository barberRepository;
    private final ApplicationUserRepository userRepository;
    private final BarberServiceRepository barberServiceRepository; // Pour les prestations
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final ClientFavoriteRepository clientFavoriteRepository;
    private final AvailabilityService availabilityService;

    @Override
    public BarberDto save(BarberDto dto) {
        // 0) Validation fonctionnelle
        var errors = BarberValidator.validate(dto);
        if (!errors.isEmpty()) {
            log.error("Barber is not valid: {}", errors);
            throw new InvalideEntityException("Barber is not valid", ErrorCodes.BARBER_NOT_VALID, errors);
        }

        // 1) Résoudre le userId (priorité à dto.userId, fallback dto.userDto.id)
        Long resolvedUserId = (dto.getUserId() != null)
                ? dto.getUserId()
                : (dto.getUserDto() != null ? dto.getUserDto().getId() : null);

        if (resolvedUserId == null) {
            throw new InvalideEntityException("Barber must be associated to a user", ErrorCodes.BARBER_NOT_VALID);
        }

        // 2) Charger l’utilisateur
        ApplicationUser user = userRepository.findById(resolvedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + resolvedUserId));

        // 3) Création vs Mise à jour + pré-check d’unicité (1 barber par userId)
        final boolean isCreate = (dto.getId() == null);
        if (isCreate) {
            if (barberRepository.existsByUser_Id(resolvedUserId)) {
                throw new ConflictException("Cet utilisateur possède déjà un profil barbier.");
            }
            Barber entity = new Barber();
            mapDtoToEntity(dto, entity);
            entity.setUser(user);
            Barber saved = barberRepository.save(entity);
            return BarberDto.fromEntity(saved);

        } else {
            // update
            Barber entity = barberRepository.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Barber not found with id " + dto.getId()));

            if (barberRepository.existsByUserIdAndIdNot(resolvedUserId, dto.getId())) {
                throw new ConflictException("Cet utilisateur possède déjà un autre profil barbier.");
            }

            mapDtoToEntity(dto, entity);
            entity.setUser(user);
            Barber saved = barberRepository.save(entity);
            return BarberDto.fromEntity(saved);
        }
    }

    /** Mapping centralisé des champs simples du DTO vers l'entité. */
    private void mapDtoToEntity(BarberDto dto, Barber entity) {
        // Adapte exactement aux champs de ton modèle/DTO
        entity.setName(dto.getSalonName());
        entity.setSalonName(dto.getSalonName());
        entity.setLocation(dto.getLocation());
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        entity.setDescription(dto.getDescription());
        entity.setSpecialties(dto.getSpecialties());
        entity.setPriceFrom(dto.getPriceFrom());
        entity.setAvailable(dto.getAvailable() != null ? dto.getAvailable() : Boolean.TRUE);
        entity.setWorkingHours(dto.getWorkingHours());
        entity.setTotalReviews(dto.getTotalReviews());
        entity.setRating(dto.getRating());
        entity.setReviewsCount(dto.getReviewsCount());
        entity.setExperience(dto.getExperience()); // si présent dans ton entité
        entity.setProfileImageUrl(dto.getProfilePicture()); // si présent dans ton entité
        // entity.setBarberId(dto.getBarberId()); // seulement si tu as ce champ
    }

    @Override
    public BarberDto findById(Long id) {
        if (id == null) {
            log.error("Barber ID is null");
            return null;
        }
        return barberRepository.findById(id)
                .map(BarberDto::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException("No barber found with ID " + id));
    }

    @Override
    public BarberDto findByUserId(Long userId) {
        if (userId == null) {
            log.error("User ID is null when looking for barber");
            return null;
        }
        return barberRepository.findByUser_Id(userId)
                .map(BarberDto::fromEntity)
                .orElseThrow(() -> new InvalideEntityException(
                        "No barber found with user ID " + userId, ErrorCodes.BARBER_NOT_FOUND));
    }

    @Override
    public List<BarberDto> findAll() {
        return barberRepository.findAll().stream().map(BarberDto::fromEntity).toList();
    }

    @Override
    public List<BarberDto> findAllAvailable() {
        return barberRepository.findByAvailable(true).stream().map(BarberDto::fromEntity).toList();
    }

    // -------- paginées / triables --------

    @Override
    public List<BarberDto> findByLocation(String location, Pageable pageable) {
        return barberRepository.findByLocationContaining(location, pageable)
                .map(BarberDto::fromEntity)
                .getContent();
    }

    @Override
    public List<BarberDto> findBySalonName(String salonName, Pageable pageable) {
        return barberRepository.findBySalonNameContaining(salonName, pageable)
                .map(BarberDto::fromEntity)
                .getContent();
    }

    @Override
    public List<BarberDto> findByPriceRange(BigDecimal min, BigDecimal max, Pageable pageable) {
        return barberRepository.findByPriceRange(min, max, pageable)
                .map(BarberDto::fromEntity)
                .getContent();
    }

    @Override
    public List<BarberDto> findByMinRating(BigDecimal minRating, Pageable pageable) {
        return barberRepository.findByMinRatingOrderByRatingDesc(minRating, pageable)
                .map(BarberDto::fromEntity)
                .getContent();
    }

    @Override
    public List<BarberDto> findAvailableByLocation(String location, Pageable pageable) {
        return barberRepository.findAvailableByLocation(location, pageable)
                .map(BarberDto::fromEntity)
                .getContent();
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

    @Override
    public List<BarberDto> findNearby(double lat, double lng, double radiusInKm) {
        // 1. Récupérer les entités via la requête SQL native
        List<Barber> barbers = barberRepository.findBarbersNearby(lat, lng, radiusInKm);

        List<BarberDto> result = new ArrayList<>();

        for (Barber barber : barbers) {
            // 2. Utiliser TA méthode statique pour la conversion de base
            BarberDto dto = BarberDto.fromEntity(barber);

            // 3. Calculer la distance précise en Java
            // On convertit les BigDecimal en double pour le calcul
            double bLat = barber.getLatitude() != null ? barber.getLatitude().doubleValue() : 0.0;
            double bLng = barber.getLongitude() != null ? barber.getLongitude().doubleValue() : 0.0;

            double distanceKm = calculateHaversineDistance(lat, lng, bLat, bLng);

            // 4. Injecter la distance dans le DTO
            // On écrase la valeur car l'entité Barber n'a probablement pas la distance
            // stockée en dur
            dto.setDistance(Math.round(distanceKm * 10.0) / 10.0); // Arrondi à 1 décimale (ex: 2.4)

            result.add(dto);
        }

        // 5. Trier du plus proche au plus loin
        result.sort(Comparator.comparing(BarberDto::getDistance));

        return result;
    }

    // --- Méthode utilitaire pour le calcul mathématique (Privée) ---
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Rayon de la Terre en km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Résultat en Km
    }

    // --- NOUVELLE MÉTHODE D'AGRÉGATION ---
    @Override
    @Transactional(readOnly = true)
    public BarberFullProfileDto getBarberFullProfile(Long barberId, Long currentClientId) {

        // 1. Récupérer le Barbier
        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barbier introuvable"));

        // 2. Récupérer les Services (Prestations)
        List<ServiceDto> services = barberServiceRepository.findByBarberId(barberId).stream()
                .map(this::mapServiceToDto) // Méthode privée à créer plus bas
                .toList();

        // 3. Récupérer le Portfolio (Photos)
        List<Post> portfolio = postRepository.findByBarberIdOrderByCreatedAtDesc(barberId);

        // 4. Récupérer les 5 derniers Avis
        List<ReviewDto> reviews = reviewRepository.findByBarberId(barberId, PageRequest.of(0, 5))
                .getContent() // ✅ 1. On récupère la List<Review> depuis la Page
                .stream() // ✅ 2. On a maintenant un Stream<Review>
                .map(this::mapReviewToDto) // ✅ 3. Ça matche maintenant avec (Review r)
                .toList();

        // 5. Vérifier si c'est un favori (Seulement si un client est connecté)
        boolean isFavorite = false;
        if (currentClientId != null) {
            isFavorite = clientFavoriteRepository.existsByClientIdAndBarberId(currentClientId, barberId);
        }

        // 6. Vérifier s'il est ouvert maintenant (Bonus)
        boolean isOpen = availabilityService.isShopOpenNow(barberId); // Supposez que vous avez cette méthode ou
                                                                      // similaire

        // 7. Assemblage final
        return BarberFullProfileDto.builder()
                .id(barber.getId())
                .salonName(barber.getSalonName())
                .firstName(barber.getUser().getFirstName())
                .lastName(barber.getUser().getLastName())
                .email(barber.getUser().getEmail())
                .phone(barber.getUser().getPhone())
                .address(barber.getLocation()) // ou getAddress() selon votre modèle
                .avatarUrl(barber.getUser().getProfileImageUrl())
                .bio(barber.getBio() != null ? barber.getBio() : "")
                .rating(barber.getRating())
                .reviewCount(barber.getReviewsCount())
                .isFavorite(isFavorite)
                .services(services)
                .portfolio(portfolio)
                .recentReviews(reviews)
                .isOpenNow(isOpen)
                .build();
    }

    // --- MAPPERS PRIVÉS (Pour éviter les dépendances circulaires entre services)
    // ---

    private ServiceDto mapServiceToDto(BarberService s) {
        return ServiceDto.builder()
                .id(s.getId())
                .name(s.getName())
                .price(s.getPrice())
                .duration(s.getDuration())
                .imageUrl(s.getImageUrl())
                .build();
    }

    // À ajouter dans BarberServiceImpl.java, à la fin de la classe

    private ReviewDto mapReviewToDto(Review r) {
        // On utilise le builder ou le constructeur vide
        ReviewDto dto = new ReviewDto();

        dto.setId(r.getId());
        dto.setOverallRating(r.getOverallRating());
        dto.setComment(r.getComment());
        dto.setCreatedAt(r.getCreatedAt());

        // Infos Client (Sécurité null-check)
        if (r.getClient() != null && r.getClient().getUser() != null) {
            dto.setClientFirstName(r.getClient().getUser().getFirstName());
            dto.setClientLastName(r.getClient().getUser().getLastName());
            dto.setClientAvatar(r.getClient().getUser().getProfileImageUrl());
        }

        // Pour les listes JSON (tags/photos), on renvoie des listes vides par défaut
        // pour ne pas alourdir le service "Profil".
        // Si vous voulez les vrais tags, il faudrait injecter ObjectMapper ici aussi.
        dto.setTags(new ArrayList<>());
        dto.setPhotos(new ArrayList<>());

        return dto;
    }
}
