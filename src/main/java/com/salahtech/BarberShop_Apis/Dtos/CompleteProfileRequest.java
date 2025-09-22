package com.salahtech.BarberShop_Apis.Dtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteProfileRequest {
    // Infos communes
    private String phone;
    private String profileImageUrl;
    private String preferences;

    // Pour Barber uniquement
    private String salonName;
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String description;
    private String specialties;
    private BigDecimal priceFrom;
    private String workingHours;
}
