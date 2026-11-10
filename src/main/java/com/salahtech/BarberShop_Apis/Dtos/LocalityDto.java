package com.salahtech.BarberShop_Apis.Dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Localité (nœud) + chemin depuis le pays, et compteurs pour la console admin. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalityDto {
    private Long id;
    private String type;
    private String typeLabel;
    private String name;
    private Long parentId;
    private boolean active;
    private boolean suggested;
    /** Niveau suivant (« Quartier » sous une ville), null pour un quartier. */
    private String childType;
    /** Chemin du pays jusqu'à ce nœud inclus (rempli par /path et sur les fiches). */
    private List<LocalityRefDto> path;
    /** Console admin : nombre d'enfants, de salons et d'utilisateurs rattachés directement. */
    private Long childrenCount;
    private Long salonsCount;
    private Long usersCount;
}
