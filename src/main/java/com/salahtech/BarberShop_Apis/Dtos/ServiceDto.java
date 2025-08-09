package com.salahtech.BarberShop_Apis.Dtos;


import com.salahtech.BarberShop_Apis.models.BarberService;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.salahtech.BarberShop_Apis.Enums.ServiceCategory;

/**
 * DTO for the Service entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDto {

    private Long id;

    @NotNull(message = "Service name is required")
    @Size(min = 2, max = 100, message = "Service name must be between 2 and 100 characters")
    private String name;

    @NotNull(message = "Barber ID is required")
    private Long barberId;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer duration;

    @NotNull(message = "Category is required")
    private ServiceCategory category;

    private BarberDto barberDto;
    @Builder.Default
    private Boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Champs pour la réponse
    private String barberName;
    private String salonName;

    public static ServiceDto fromService(BarberService barberService) {
        return ServiceDto.builder()
                .id(barberService.getId())
                .name(barberService.getName())
                .description(barberService.getDescription())
                .price(barberService.getPrice())
                .duration(barberService.getDuration())
                .category(barberService.getCategory())
                //.barberDto(BarberDto.fromEntity(service.getBarber()))
                .active(barberService.getActive())
                .createdAt(barberService.getCreatedAt())
                .updatedAt(barberService.getUpdatedAt())
                .build();
    }

    public static BarberService toEntity(ServiceDto dto) {

        if (dto == null) {
            return null;
        }

        BarberService barberService = new BarberService();
        barberService.setId(dto.id);
        barberService.setName(dto.name);
        barberService.setDescription(dto.description);
        barberService.setPrice(dto.price);
        barberService.setDuration(dto.duration);
        barberService.setCategory(dto.category);
        barberService.setBarber(BarberDto.toEntity(dto.getBarberDto())); // Assuming Barber has a constructor that accepts ID
        barberService.setActive(dto.active);
        barberService.setCreatedAt(LocalDateTime.now());
        barberService.setUpdatedAt(LocalDateTime.now()); // Assuming updatedAt is set to current time
        return barberService;
    }
    
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();   
    }

    /**
     * Updates the service entity with values from this DTO.
     *
     * @param service the service entity to update
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
}

