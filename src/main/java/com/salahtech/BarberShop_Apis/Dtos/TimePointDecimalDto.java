package com.salahtech.BarberShop_Apis.Dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimePointDecimalDto {
    private LocalDate date;
    private BigDecimal value;
}