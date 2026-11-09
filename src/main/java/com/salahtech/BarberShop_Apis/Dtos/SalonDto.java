package com.salahtech.BarberShop_Apis.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonDto {
    private Long id;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String phoneNumber;

    /** Branding. */
    private String logoUrl;
    private String coverUrl;

    private Long ownerId;
    private String ownerName;

    private Long establishmentTypeId;
    private String establishmentTypeName;

    // Compteurs utiles pour le portail de gestion
    private long barbersCount;
    private long workstationsCount;
}
