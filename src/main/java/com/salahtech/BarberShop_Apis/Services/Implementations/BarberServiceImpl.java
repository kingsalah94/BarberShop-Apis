package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.AOP.LogActivity; // 👈 IMPORT AOP
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
import com.salahtech.BarberShop_Apis.models.*;
import com.salahtech.BarberShop_Apis.reppsitories.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BarberServiceImpl implements BarberServices {

    private final BarberRepository barberRepository;
    private final ApplicationUserRepository userRepository;
    private final BarberServiceRepository barberServiceRepository;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final ClientFavoriteRepository clientFavoriteRepository;
    private final AvailabilityService availabilityService;
    private final FileStorageService fileStorageService;

    // ======================================================
    // ✅ MÉTHODE CREATE (Spécifique avec Image)
    // ======================================================
    @Override
    @Transactional
    @LogActivity(action = "CREATE_PROFILE", entityName = "Barber") // ✅ AUDIT AJOUTÉ
    public BarberDto create(BarberDto dto, String imageUrl) {
        // Validation
        var errors = BarberValidator.validate(dto);
        if (!errors.isEmpty()) {
            throw new InvalideEntityException("Barber is not valid", ErrorCodes.BARBER_NOT_VALID, errors);
        }

        // Résolution User
        Long resolvedUserId = (dto.getUserId() != null) ? dto.getUserId()
                : (dto.getUserDto() != null ? dto.getUserDto().getId() : null);
        if (resolvedUserId == null)
            throw new InvalideEntityException("User ID required", ErrorCodes.BARBER_NOT_VALID);

        ApplicationUser user = userRepository.findById(resolvedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + resolvedUserId));

        // Check Unicité
        if (barberRepository.existsByUser_Id(resolvedUserId)) {
            throw new ConflictException("Cet utilisateur a déjà un profil barbier.");
        }

        Barber entity = new Barber();
        mapDtoToEntity(dto, entity);
        entity.setUser(user);

        if (imageUrl != null) {
            entity.setProfileImageUrl(imageUrl);
        }

        Barber savedBarber = barberRepository.save(entity);
        return BarberDto.fromEntity(savedBarber); // L'aspect récupérera l'ID depuis ce retour
    }

    // ======================================================
    // ✅ MÉTHODE UPDATE (Spécifique avec Image)
    // ======================================================
    @Override
    @Transactional
    @LogActivity(action = "UPDATE_PROFILE", entityName = "Barber") // ✅ AUDIT AJOUTÉ
    public BarberDto update(Long id, BarberDto dto, String newImageUrl) {
        Barber barber = barberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found ID: " + id));

        // Validation simple
        if (dto.getSalonName() == null || dto.getSalonName().isBlank()) {
            throw new InvalideEntityException("Le nom du salon est requis");
        }

        // Mapping (On ne touche pas à l'User ni à l'ID)
        barber.setSalonName(dto.getSalonName());
        barber.setLocation(dto.getLocation());
        barber.setLatitude(dto.getLatitude());
        barber.setLongitude(dto.getLongitude());
        barber.setDescription(dto.getDescription());
        barber.setSpecialties(dto.getSpecialties());
        barber.setPriceFrom(dto.getPriceFrom());
        barber.setAvailable(dto.getAvailable());
        barber.setWorkingHours(dto.getWorkingHours());
        barber.setExperience(dto.getExperience());

        if (newImageUrl != null && !newImageUrl.isEmpty()) {
            barber.setProfileImageUrl(newImageUrl);
        }

        Barber savedBarber = barberRepository.save(barber);
        return BarberDto.fromEntity(savedBarber);
    }

    // Garder cette méthode pour compatibilité si nécessaire (appel interne)
    @Override
    public BarberDto save(BarberDto dto) {
        // Attention : Si cette méthode est appelée depuis un contrôleur, l'audit
        // ne se déclenchera pas sur create/update à cause de l'appel interne (this.).
        // Si tu utilises save() comme point d'entrée, ajoute @LogActivity ici aussi.
        if (dto.getId() == null)
            return create(dto, null);
        else
            return update(dto.getId(), dto, null);
    }

    // ... LE RESTE DU FICHIER RESTE IDENTIQUE ...

    private void mapDtoToEntity(BarberDto dto, Barber entity) {
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
        entity.setExperience(dto.getExperience());
    }

    @Override
    public BarberDto findById(Long id) {
        if (id == null)
            return null;
        return barberRepository.findById(id)
                .map(BarberDto::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException("No barber found with ID " + id));
    }

    @Override
    public BarberDto findByUserId(Long userId) {
        if (userId == null)
            return null;
        return barberRepository.findByUser_Id(userId)
                .map(BarberDto::fromEntity)
                .orElseThrow(() -> new InvalideEntityException("No barber found with user ID " + userId));
    }

    @Override
    public List<BarberDto> findAll() {
        return barberRepository.findAll().stream().map(BarberDto::fromEntity).toList();
    }

    @Override
    public List<BarberDto> findAllAvailable() {
        return barberRepository.findByAvailable(true).stream().map(BarberDto::fromEntity).toList();
    }

    @Override
    public List<BarberDto> findByLocation(String location, Pageable pageable) {
        return barberRepository.findByLocationContaining(location, pageable).map(BarberDto::fromEntity).getContent();
    }

    @Override
    public List<BarberDto> findBySalonName(String salonName, Pageable pageable) {
        return barberRepository.findBySalonNameContaining(salonName, pageable).map(BarberDto::fromEntity).getContent();
    }

    @Override
    public List<BarberDto> findByPriceRange(BigDecimal min, BigDecimal max, Pageable pageable) {
        return barberRepository.findByPriceRange(min, max, pageable).map(BarberDto::fromEntity).getContent();
    }

    @Override
    public List<BarberDto> findByMinRating(BigDecimal minRating, Pageable pageable) {
        return barberRepository.findByMinRatingOrderByRatingDesc(minRating, pageable).map(BarberDto::fromEntity)
                .getContent();
    }

    @Override
    public List<BarberDto> findAvailableByLocation(String location, Pageable pageable) {
        return barberRepository.findAvailableByLocation(location, pageable).map(BarberDto::fromEntity).getContent();
    }

    @Override
    @LogActivity(action = "DELETE_PROFILE", entityName = "Barber") // ✅ AUDIT AJOUTÉ
    public void delete(Long id) {
        if (id == null)
            return;
        barberRepository.deleteById(id);
    }

    @Override
    public List<BarberDto> findNearby(double lat, double lng, double radiusInKm) {
        List<Barber> barbers = barberRepository.findBarbersNearby(lat, lng, radiusInKm);
        List<BarberDto> result = new ArrayList<>();
        for (Barber barber : barbers) {
            BarberDto dto = BarberDto.fromEntity(barber);
            double bLat = barber.getLatitude() != null ? barber.getLatitude().doubleValue() : 0.0;
            double bLng = barber.getLongitude() != null ? barber.getLongitude().doubleValue() : 0.0;
            double distanceKm = calculateHaversineDistance(lat, lng, bLat, bLng);
            dto.setDistance(Math.round(distanceKm * 10.0) / 10.0);
            result.add(dto);
        }
        result.sort(Comparator.comparing(BarberDto::getDistance));
        return result;
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    @Transactional(readOnly = true)
    public BarberFullProfileDto getBarberFullProfile(Long barberId, Long currentClientId) {
        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barbier introuvable"));

        List<ServiceDto> services = barberServiceRepository.findByBarberId(barberId).stream()
                .map(this::mapServiceToDto).toList();

        List<Post> portfolio = postRepository.findByBarberIdOrderByCreatedAtDesc(barberId);

        List<ReviewDto> reviews = reviewRepository.findByBarberId(barberId, PageRequest.of(0, 5))
                .getContent().stream().map(this::mapReviewToDto).toList();

        boolean isFavorite = false;
        if (currentClientId != null) {
            isFavorite = clientFavoriteRepository.existsByClientIdAndBarberId(currentClientId, barberId);
        }

        boolean isOpen = availabilityService.isShopOpenNow(barberId);

        return BarberFullProfileDto.builder()
                .id(barber.getId())
                .salonName(barber.getSalonName())
                .firstName(barber.getUser().getFirstName())
                .lastName(barber.getUser().getLastName())
                .email(barber.getUser().getEmail())
                .phone(barber.getUser().getPhone())
                .address(barber.getLocation())
                .avatarUrl(barber.getProfileImageUrl())
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

    private ServiceDto mapServiceToDto(BarberService s) {
        return ServiceDto.builder()
                .id(s.getId())
                .name(s.getName())
                .price(s.getPrice())
                .duration(s.getDuration())
                .imageUrl(s.getImageUrl())
                .build();
    }

    private ReviewDto mapReviewToDto(Review r) {
        ReviewDto dto = new ReviewDto();
        dto.setId(r.getId());
        dto.setOverallRating(r.getOverallRating());
        dto.setComment(r.getComment());
        dto.setCreatedAt(r.getCreatedAt());
        if (r.getClient() != null && r.getClient().getUser() != null) {
            dto.setClientFirstName(r.getClient().getUser().getFirstName());
            dto.setClientLastName(r.getClient().getUser().getLastName());
            dto.setClientAvatar(r.getClient().getUser().getProfileImageUrl());
        }
        dto.setTags(new ArrayList<>());
        dto.setPhotos(new ArrayList<>());
        return dto;
    }

    @Override
    public BarberDto findBarberByUserId(Long userId) {
        return barberRepository.findByUser_Id(userId)
                .map(BarberDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found for user ID: " + userId));
    }

}