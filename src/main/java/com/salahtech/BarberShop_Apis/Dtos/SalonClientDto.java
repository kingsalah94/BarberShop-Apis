package com.salahtech.BarberShop_Apis.Dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Client d'un salon (agrégé depuis ses réservations). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonClientDto {
    private Long clientId;
    private String name;
    private String email;
    private String phone;
    private long totalBookings;
    private BigDecimal totalSpent;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastVisit;

    /** Note privée du salon sur ce client (null si aucune). */
    private String note;
}
