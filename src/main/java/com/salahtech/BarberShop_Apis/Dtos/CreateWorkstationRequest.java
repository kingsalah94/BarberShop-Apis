package com.salahtech.BarberShop_Apis.Dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateWorkstationRequest {

    @NotBlank(message = "Le nom est requis")
    private String name;

    private String description;

    @NotNull(message = "Le salon est requis")
    private Long salonId;

    private Integer displayOrder;
}