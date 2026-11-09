package com.salahtech.BarberShop_Apis.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Fiche complète d'un salon pour le client : infos + services actifs + horaires. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonCatalogDetailDto {
    private Long id;
    private String name;
    private String address;
    private String phoneNumber;
    private Double latitude;
    private Double longitude;
    private String establishmentTypeName;
    private Double rating;
    private long reviewsCount;
    private List<SalonServiceDto> services;
    private List<SalonHoursDto> hours;
}
