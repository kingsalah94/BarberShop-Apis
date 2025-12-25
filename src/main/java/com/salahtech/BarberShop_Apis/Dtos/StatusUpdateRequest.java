package com.salahtech.BarberShop_Apis.Dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusUpdateRequest {
    @NotNull(message = "Le statut actif est requis")
    private Boolean active;
}