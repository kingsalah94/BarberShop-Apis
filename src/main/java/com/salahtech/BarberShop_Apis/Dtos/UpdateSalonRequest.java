package com.salahtech.BarberShop_Apis.Dtos;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mise à jour partielle : seuls les champs non-null sont appliqués. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSalonRequest {

    @Size(max = 150)
    private String name;

    private String address;
    private Double latitude;
    private Double longitude;
    private String phoneNumber;
    private Long establishmentTypeId;
}
