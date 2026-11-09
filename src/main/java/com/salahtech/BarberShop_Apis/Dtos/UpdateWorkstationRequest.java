package com.salahtech.BarberShop_Apis.Dtos;

import lombok.Data;

@Data
public class UpdateWorkstationRequest {
    private String name;
    private String description;
    private Integer displayOrder;
    private Boolean isActive;
}