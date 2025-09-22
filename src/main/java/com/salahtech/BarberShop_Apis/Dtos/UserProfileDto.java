package com.salahtech.BarberShop_Apis.Dtos;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileDto {
    private ApplicationUserDto user;
    private ClientDto client;
    private BarberDto barber;
    private boolean profileComplete;
    private LocalDateTime lastActivity;
    private ProfileStats stats;

}
