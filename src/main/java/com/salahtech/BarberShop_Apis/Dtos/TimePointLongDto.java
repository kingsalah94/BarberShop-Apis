package com.salahtech.BarberShop_Apis.Dtos;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimePointLongDto {
    private LocalDate date;
    private Long value;
}
