package com.salahtech.BarberShop_Apis.Dtos;

import java.util.List;

import com.salahtech.BarberShop_Apis.Enums.DaysOfWeek;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayAvailabilityDto {
    private DaysOfWeek dayOfWeek;
    private boolean isAvailable;
    private List<TimeSlotDto> timeSlots;
}
