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

    /** Localité la plus précise (quartier, sinon ville…) et son chemin depuis le pays. */
    private Long localityId;
    private java.util.List<LocalityRefDto> localityPath;
    private Double latitude;
    private Double longitude;
    private String establishmentTypeName;
    private Double rating;
    private long reviewsCount;
    /** Photos du salon (null si non renseignées). */
    private String logoUrl;
    private String coverUrl;
    /** Prix de la prestation active la moins chère (« à partir de »). */
    private java.math.BigDecimal minPrice;
    /** Catégories des prestations actives (filtres « Coupe », « Barbe »…). */
    private java.util.List<String> categories;
    /** Ouvert maintenant ; horaires effectifs du jour (null = fermé aujourd'hui). */
    private boolean openNow;
    private String todayOpenAt;
    private String todayCloseAt;
    private List<SalonServiceDto> services;
    private List<SalonHoursDto> hours;
}
