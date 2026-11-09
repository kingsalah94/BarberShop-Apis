package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.StationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkstationDto {
    private Long id;
    private String name;
    private String description;
    private StationStatus status;
    private String statusDisplayName;
    private Long salonId;
    private Long currentEmployeeId;
    private String currentEmployeeName;
    private Long currentBarberId;
    private String currentBarberName;
    private Integer displayOrder;
    // Boxed Boolean : Lombok génère getIsActive() => Jackson sérialise la clé "isActive"
    // attendue par le front (un boolean primitif donnerait la clé "active").
    private Boolean isActive;
}