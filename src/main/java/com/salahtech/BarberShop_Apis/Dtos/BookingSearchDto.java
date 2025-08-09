package com.salahtech.BarberShop_Apis.Dtos;

import java.time.LocalDateTime;

import com.salahtech.BarberShop_Apis.Enums.BookingStatus;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingSearchDto {

    private Long userId;
    private Long barberId;
    private Long serviceId;
    
    private BookingStatus status;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    // Paramètres de tri
    private String sortBy; // date, price, status
    private String sortDirection; // asc, desc
}
