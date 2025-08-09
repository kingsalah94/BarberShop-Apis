package com.salahtech.BarberShop_Apis.Dtos;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;


import com.salahtech.BarberShop_Apis.models.Availability;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvailabilityDto {

    private Long id;
    private Long barberId;
    private LocalDateTime date;
    
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isAvailable;
    private String note;
    private List<TimeSlotDto> availableSlots;
    private List<TimeSlotDto> bookedSlots;
    
    public static AvailabilityDto fromEntity(Availability availability) {
        if (availability == null) return null;
        
        return AvailabilityDto.builder()
                .id(availability.getId())
                .barberId(availability.getBarber().getId())
                .dayOfWeek(availability.getDayOfWeek())
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .isAvailable(availability.getIsAvailable())
                .note(availability.getNote())
                .build();
    }
    
    public static Availability toEntity(AvailabilityDto dto) {
        if (dto == null) return null;
        
        Availability availability = new Availability();
        availability.setId(dto.getId());
        availability.setDayOfWeek(dto.getDayOfWeek());
        availability.setStartTime(dto.getStartTime());
        availability.setEndTime(dto.getEndTime());
        availability.setIsAvailable(dto.getIsAvailable());
        availability.setNote(dto.getNote());
        return availability;
    }

    public AvailabilityDto(Long barberId2, LocalDateTime atStartOfDay, Object object, Object object2) {
        //TODO Auto-generated constructor stub
    }
}
