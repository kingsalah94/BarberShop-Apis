package com.salahtech.BarberShop_Apis.Dtos;

import java.util.Map;

import com.salahtech.BarberShop_Apis.Enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private String recipient;
    private String subject;
    private String message;
    private NotificationType type;
    private Map<String, String> data;
}
