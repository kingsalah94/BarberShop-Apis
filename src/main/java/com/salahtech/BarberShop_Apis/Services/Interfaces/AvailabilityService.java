package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.AvailabilityDto;
import com.salahtech.BarberShop_Apis.Dtos.TimeSlotDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AvailabilityService extends BaseService<AvailabilityDto, Long> {
    
    List<TimeSlotDto> getAvailableSlots(Long barberId, LocalDate date, Integer serviceDuration);
    
    boolean isSlotAvailable(Long barberId, LocalDateTime startTime, LocalDateTime endTime);
    
    void blockTimeSlot(Long barberId, LocalDateTime startTime, LocalDateTime endTime, String reason);
    
    void releaseTimeSlot(Long barberId, LocalDateTime startTime, LocalDateTime endTime);
    
    List<AvailabilityDto> getWeeklyAvailability(Long barberId, LocalDate weekStart);
    
    void updateWeeklySchedule(Long barberId, List<AvailabilityDto> weeklySchedule);
}
