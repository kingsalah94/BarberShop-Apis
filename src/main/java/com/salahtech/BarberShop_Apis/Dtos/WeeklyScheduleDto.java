package com.salahtech.BarberShop_Apis.Dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyScheduleDto {
    private Long id;
    private Long barberId;
    private List<DayAvailabilityDto> weeklySchedule;
    private String createdAt;
    private String updatedAt;
}
