package com.salahtech.BarberShop_Apis.Services.Interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.salahtech.BarberShop_Apis.Dtos.ReviewRequestDto;
import com.salahtech.BarberShop_Apis.Dtos.ReviewResponseDto;

public interface ReviewService {

    /**
     * Créer un avis
     */
    ReviewResponseDto createReview(Long clientId, ReviewRequestDto dto);

    /**
     * Récupérer les avis d'un barbier (Public)
     */
    Page<ReviewResponseDto> getBarberReviews(Long barberId, Pageable pageable);

    /**
     * Supprimer un avis (Soft Delete)
     */
    void deleteReview(Long reviewId, Long clientId);

}
