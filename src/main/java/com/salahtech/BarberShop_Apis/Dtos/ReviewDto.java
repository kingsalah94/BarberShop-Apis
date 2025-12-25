package com.salahtech.BarberShop_Apis.Dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.salahtech.BarberShop_Apis.models.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewDto {

    private Long id;

    // --- Relations (IDs) ---
    private Long bookingId;
    private Long barberId;
    private Long clientId;

    // --- Infos Client (Pour l'affichage direct) ---
    private String clientFirstName;
    private String clientLastName;
    private String clientAvatar;

    // --- Ratings ---
    private Integer overallRating;
    private Integer qualityRating;
    private Integer receptionRating;
    private Integer valueForMoneyRating;
    private Integer punctualityRating;

    // --- Contenu ---
    private String comment;

    // ⚠️ Transformation : L'entité a des String (JSON), le DTO renvoie des Listes
    private List<String> tags;
    private List<String> photos;

    private BigDecimal tipAmount;

    // --- Dates ---
    private LocalDateTime createdAt;

    // Champ calculé optionnel (ex: "Il y a 2 jours")
    private String timeAgo;

    /**
     * Convertit une Entité Review en DTO.
     * ⚠️ Note : Pour 'tags' et 'photos', comme ce sont des JSON String dans
     * l'entité,
     * il vaut mieux gérer leur conversion dans le Service avec un ObjectMapper.
     * Ici, on initialise des listes vides par défaut pour éviter les null.
     */
    public static ReviewDto fromEntity(Review review) {
        if (review == null)
            return null;

        return ReviewDto.builder()
                .id(review.getId())
                .bookingId(review.getBooking() != null ? review.getBooking().getId() : null)
                .barberId(review.getBarber() != null ? review.getBarber().getId() : null)
                .clientId(review.getClient() != null ? review.getClient().getId() : null)

                // Récupération sécurisée des infos client via User
                .clientFirstName(review.getClient() != null && review.getClient().getUser() != null
                        ? review.getClient().getUser().getFirstName()
                        : "")
                .clientLastName(review.getClient() != null && review.getClient().getUser() != null
                        ? review.getClient().getUser().getLastName()
                        : "")
                .clientAvatar(review.getClient() != null && review.getClient().getUser() != null
                        ? review.getClient().getUser().getProfileImageUrl()
                        : null)

                .overallRating(review.getOverallRating())
                .qualityRating(review.getQualityRating())
                .receptionRating(review.getReceptionRating())
                .valueForMoneyRating(review.getValueForMoneyRating())
                .punctualityRating(review.getPunctualityRating())

                .comment(review.getComment())
                .tipAmount(review.getTipAmount())
                .createdAt(review.getCreatedAt())

                // Les listes seront remplies par le Service via Jackson (ObjectMapper)
                // car parser du JSON dans un DTO statique est risqué (Exceptions)
                .tags(new ArrayList<>())
                .photos(new ArrayList<>())
                .build();
    }
}