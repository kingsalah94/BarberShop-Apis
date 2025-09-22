package com.salahtech.BarberShop_Apis.Dtos;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileIncompleteError {
    private String error;
    private String message;
    private LocalDateTime timestamp;
    private String path;
    private String redirectTo;
}
