package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.ServiceCategory;
import com.salahtech.BarberShop_Apis.models.BarberService;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDto {

    private Long id;

    @NotNull
    @Size(min = 2, max = 100)
    private String name;

    @NotNull
    private Long barberId;

    @Size(max = 500)
    private String description;

    private String imageUrl;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @NotNull
    @Min(1)
    private Integer duration;

    @NotNull
    private ServiceCategory category;

    @Builder.Default
    private Boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // lecture
    private String barberName;
    private String salonName;

    public static ServiceDto fromService(BarberService s) {
        if (s == null)
            return null;
        ServiceDto dto = ServiceDto.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .price(s.getPrice())
                .duration(s.getDuration())
                .category(s.getCategory())
                .active(s.getActive())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .imageUrl(s.getImageUrl())

                .build();

        if (s.getBarber() != null) {
            dto.setBarberId(s.getBarber().getId());
            // ← Ajoute ces 2 lignes qui manquaient
            dto.setBarberName(s.getBarber().getName());
            dto.setSalonName(s.getBarber().getSalonName());
        }

        return dto;
    }

    /** DTO -> entity (sans lier le barber ici). */
    public static BarberService toEntity(ServiceDto dto) {
        if (dto == null)
            return null;
        BarberService s = new BarberService();
        s.setId(dto.getId());
        s.setName(dto.getName());
        s.setDescription(dto.getDescription());
        s.setPrice(dto.getPrice());
        s.setDuration(dto.getDuration());
        s.setCategory(dto.getCategory());
        s.setActive(dto.getActive());
        s.setImageUrl(dto.getImageUrl());
        // barber + dates gérés par service/JPA
        return s;
    }
}
