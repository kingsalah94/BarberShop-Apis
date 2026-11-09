package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.ReplyRequestDto;
import com.salahtech.BarberShop_Apis.Dtos.ReviewRequestDto;
import com.salahtech.BarberShop_Apis.Dtos.ReviewResponseDto;
import com.salahtech.BarberShop_Apis.Services.Implementations.ReviewServiceImpl;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ReviewService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Gestion des avis et notations")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Laisser un avis (Client uniquement)")
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ReviewResponseDto> createReview(
            @CurrentUser ApplicationUser user,
            @Valid @RequestBody ReviewRequestDto dto) {

        if (user.getClient() == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(reviewService.createReview(user.getClient().getId(), dto));
    }

    @Operation(summary = "Voir les avis d'un barbier (Public)")
    @GetMapping("/barber/{barberId}")
    public ResponseEntity<Page<ReviewResponseDto>> getBarberReviews(
            @PathVariable Long barberId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(reviewService.getBarberReviews(barberId, pageable));
    }

    @Operation(summary = "Supprimer un avis (Auteur uniquement)")
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Void> deleteReview(
            @CurrentUser ApplicationUser user,
            @PathVariable Long reviewId) {

        if (user.getClient() == null) {
            return ResponseEntity.badRequest().build();
        }

        reviewService.deleteReview(reviewId, user.getClient().getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Répondre à un avis (Barbier uniquement)")
    @PostMapping("/{reviewId}/reply")
    @PreAuthorize("hasAnyRole('BARBER', 'ADMIN')")
    public ResponseEntity<ReviewResponseDto> replyToReview(
            @CurrentUser ApplicationUser user,
            @PathVariable Long reviewId,
            @RequestBody @Valid ReplyRequestDto dto) {

        return ResponseEntity.ok(reviewService.replyToReview(reviewId, user.getId(), dto.getReply()));
    }
}