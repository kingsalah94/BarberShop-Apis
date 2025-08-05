package com.salahtech.BarberShop_Apis.Dtos;


import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.salahtech.BarberShop_Apis.Enums.ServiceCategory;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Service;

/**
 * DTO for the Service entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDto {

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer duration;

    private ServiceCategory category;

    private BarberDto barberDto;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static ServiceDto fromService(Service service) {
        return ServiceDto.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .duration(service.getDuration())
                .category(service.getCategory())
                //.barberDto(BarberDto.fromEntity(service.getBarber()))
                .active(service.getActive())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }

    public static Service toEntity(ServiceDto dto) {

        if (dto == null) {
            return null;
        }

        Service service = new Service();
        service.setId(dto.id);
        service.setName(dto.name);
        service.setDescription(dto.description);
        service.setPrice(dto.price);
        service.setDuration(dto.duration);
        service.setCategory(dto.category);
        service.setBarber(BarberDto.toEntity(dto.getBarberDto())); // Assuming Barber has a constructor that accepts ID
        service.setActive(dto.active);
        service.setCreatedAt(LocalDateTime.now());
        service.setUpdatedAt(LocalDateTime.now()); // Assuming updatedAt is set to current time
        return service;
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

