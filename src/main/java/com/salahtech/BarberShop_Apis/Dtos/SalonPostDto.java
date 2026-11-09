package com.salahtech.BarberShop_Apis.Dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Publication (réalisation) d'un salon. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonPostDto {
    private Long id;
    private Long salonId;
    private String salonName;
    private String imageUrl;
    private String caption;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
