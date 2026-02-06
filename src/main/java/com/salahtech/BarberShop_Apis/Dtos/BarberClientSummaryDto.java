package com.salahtech.BarberShop_Apis.Dtos;

import java.time.LocalDateTime;

public interface BarberClientSummaryDto {
    Long getId();

    String getFirstName();

    String getLastName();

    String getPhone();

    String getEmail();

    // ✅ On utilise le nom Java standard pour le mapping automatique
    String getProfileImageUrl();

    Long getTotalVisits();

    Double getTotalRevenue();

    LocalDateTime getLastVisit();

    LocalDateTime getNextBooking();

    LocalDateTime getRegisteredAt();

    Boolean getIsVip();
}