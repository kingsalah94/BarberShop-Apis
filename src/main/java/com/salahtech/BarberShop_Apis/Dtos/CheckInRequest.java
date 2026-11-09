package com.salahtech.BarberShop_Apis.Dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CheckInRequest {

    @NotBlank(message = "Le nom du client est requis")
    private String customerName;

    private String customerPhone;

    @Min(value = 1, message = "Au moins 1 personne")
    private int numberOfPeople = 1;

    @NotNull(message = "Le salon est requis")
    private Long salonId;

    private Long clientId;

    private Long preferredEmployeeId;

    private List<Long> requestedServiceIds;

    private String notes;
}