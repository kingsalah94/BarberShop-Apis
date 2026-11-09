package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salahtech.BarberShop_Apis.AOP.LogActivity;
import com.salahtech.BarberShop_Apis.Dtos.ReviewRequestDto;
import com.salahtech.BarberShop_Apis.Dtos.ReviewResponseDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Exceptions.ConflictException;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ReviewService;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.models.Review;
import com.salahtech.BarberShop_Apis.repositories.BarberRepository;
import com.salahtech.BarberShop_Apis.repositories.BookingRepository;
import com.salahtech.BarberShop_Apis.repositories.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final BarberRepository barberRepository;
    private final ObjectMapper objectMapper; // Pour convertir List <-> JSON String

    /**
     * Créer un avis
     */
    @Override
    public ReviewResponseDto createReview(Long clientId, ReviewRequestDto dto) {
        // 1. Récupérer le RDV
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable"));

        // 2. Vérifications de sécurité
        if (!booking.getClient().getId().equals(clientId)) {
            throw new InvalideEntityException("Ce rendez-vous ne vous appartient pas.");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new InvalideEntityException("Le rendez-vous doit être terminé (COMPLETED) pour laisser un avis.");
        }
        if (reviewRepository.existsByBookingIdAndClientIdAndIsDeletedFalse(booking.getId(), clientId)) {
            throw new ConflictException("Vous avez déjà laissé un avis pour ce rendez-vous.");
        }

        // 3. Conversion List -> JSON String
        String tagsJson = convertListToJson(dto.getTags());
        String photosJson = convertListToJson(dto.getPhotos());

        // 4. Création Entité
        Review review = Review.builder()
                .booking(booking)
                .client(booking.getClient())
                .barber(booking.getBarber())
                .overallRating(dto.getOverallRating())
                .qualityRating(dto.getQualityRating())
                .receptionRating(dto.getReceptionRating())
                .valueForMoneyRating(dto.getValueForMoneyRating())
                .punctualityRating(dto.getPunctualityRating())
                .comment(dto.getComment())
                .tags(tagsJson)
                .photos(photosJson)
                .tipAmount(dto.getTipAmount())
                .isDeleted(false)
                .build();

        reviewRepository.save(review);

        // 5. ⚡ MISE À JOUR DES STATS DU BARBIER ⚡
        updateBarberStats(booking.getBarber());

        return mapToDto(review);
    }

    /**
     * Récupérer les avis d'un barbier (Public)
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getBarberReviews(Long barberId, Pageable pageable) {
        return reviewRepository.findByBarberId(barberId, pageable)
                .map(this::mapToDto);
    }

    /**
     * Supprimer un avis (Soft Delete)
     */
    @Override
    public void deleteReview(Long reviewId, Long clientId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis introuvable"));

        // Seul l'auteur (client) ou un Admin peut supprimer
        // Ici on vérifie le client
        if (!review.getClient().getId().equals(clientId)) {
            throw new InvalideEntityException("Vous ne pouvez pas supprimer cet avis.");
        }

        review.setIsDeleted(true);
        reviewRepository.save(review);

        // Recalculer la moyenne du barbier car un avis a disparu
        updateBarberStats(review.getBarber());
    }

    // --- Helpers Privés ---

    private void updateBarberStats(Barber barber) {
        Double avg = reviewRepository.getAverageRatingForBarber(barber.getId());
        Long count = reviewRepository.countByBarberId(barber.getId());

        barber.setRating(BigDecimal.valueOf(avg != null ? avg : 0.0));
        barber.setReviewsCount(count.intValue());

        barberRepository.save(barber);
        log.info("Stats updated for Barber {}: Rating={}, Count={}", barber.getId(), avg, count);
    }

    private ReviewResponseDto mapToDto(Review r) {
        return ReviewResponseDto.builder()
                .id(r.getId())
                .clientName(r.getClient().getFirstName()) // + " " + r.getClient().getLastName()
                .clientAvatar(r.getClient().getUser().getProfileImageUrl())
                .barberId(r.getBarber().getId())
                .overallRating(r.getOverallRating())
                .qualityRating(r.getQualityRating())
                .receptionRating(r.getReceptionRating())
                .valueForMoneyRating(r.getValueForMoneyRating())
                .punctualityRating(r.getPunctualityRating())
                .comment(r.getComment())
                .tags(convertJsonToList(r.getTags()))
                .photos(convertJsonToList(r.getPhotos()))
                .tipAmount(r.getTipAmount())
                .createdAt(r.getCreatedAt())
                .barberReply(r.getBarberReply())
                .repliedAt(r.getRepliedAt())
                .build();
    }

    private String convertListToJson(List<String> list) {
        if (list == null || list.isEmpty())
            return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> convertJsonToList(String json) {
        if (json == null || json.isEmpty())
            return new ArrayList<>();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    // 1. La méthode de réponse
    @Override
    @Transactional
    @LogActivity(action = "REPLY_TO_REVIEW", entityName = "Review")
    public ReviewResponseDto replyToReview(Long reviewId, Long barberUserId, String replyText) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis introuvable"));

        // Sécurité : Vérifier que c'est bien le barbier concerné qui répond
        if (!review.getBarber().getUser().getId().equals(barberUserId)) {
            throw new InvalideEntityException("Vous ne pouvez pas répondre à un avis qui ne vous concerne pas.");
        }

        review.setBarberReply(replyText);
        review.setRepliedAt(LocalDateTime.now());

        Review saved = reviewRepository.save(review);
        return mapToDto(saved);
    }

    // 2. Mise à jour du Mapper existant (mapToDto)
    // private ReviewResponseDto mapToDto(Review r) {
    // return ReviewResponseDto.builder()
    // .id(r.getId())
    // // ... tes champs existants ...
    // .clientName(r.getClient().getUser().getFirstName() + " " +
    // r.getClient().getUser().getLastName())
    // .clientAvatar(r.getClient().getUser().getProfileImageUrl())
    // .barberId(r.getBarber().getId())
    // .overallRating(r.getOverallRating())
    // .comment(r.getComment())
    // .tags(convertJsonToList(r.getTags()))
    // .createdAt(r.getCreatedAt())
    // // ✅ AJOUTS
    // .barberReply(r.getBarberReply())
    // .repliedAt(r.getRepliedAt())
    // .build();
    // }
}