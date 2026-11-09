package com.salahtech.BarberShop_Apis.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vue résumée d'un salon pour le catalogue client. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonCatalogDto {
    private Long id;
    private String name;
    private String address;
    private String phoneNumber;
    private Double latitude;
    private Double longitude;
    private String establishmentTypeName;
    private long servicesCount;
    /** Note moyenne (null si aucun avis) + nombre d'avis. */
    private Double rating;
    private long reviewsCount;
}
