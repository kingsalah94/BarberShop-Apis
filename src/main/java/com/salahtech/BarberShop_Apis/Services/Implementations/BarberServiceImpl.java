package com.salahtech.BarberShop_Apis.Services.Implementations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.salahtech.BarberShop_Apis.Dtos.BarberDto;
import com.salahtech.BarberShop_Apis.Enums.ErrorCodes;
import com.salahtech.BarberShop_Apis.Exceptions.ConflictException;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.BarberService;
import com.salahtech.BarberShop_Apis.Validators.BarberValidator;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BarberRepository;

import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BarberServiceImpl implements BarberService {

    private final BarberRepository barberRepository;
    private final ApplicationUserRepository userRepository; // <<< final + injecté

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
            if (barberRepository.existsByUserId(resolvedUserId)) {
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
        entity.setName(dto.getName());
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
        return barberRepository.findByUserId(userId)
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
}
