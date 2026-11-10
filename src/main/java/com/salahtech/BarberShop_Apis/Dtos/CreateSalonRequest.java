package com.salahtech.BarberShop_Apis.Dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSalonRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    private String address;
    private Double latitude;
    private Double longitude;
    private String phoneNumber;
    /** Zone (quartier ou ville) du référentiel ; facultative. */
    private Long localityId;

    /** Optionnel : type d'établissement (catégories accessibles) */
    private Long establishmentTypeId;
}
