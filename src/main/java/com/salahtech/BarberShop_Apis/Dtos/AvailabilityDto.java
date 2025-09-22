package com.salahtech.BarberShop_Apis.Dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.salahtech.BarberShop_Apis.models.Availability;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvailabilityDto {
    private Long id;
    private Long barberId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date; // one-off

    @JsonFormat(pattern = "EEEE") // Full name of the day
    private DayOfWeek dayOfWeek; // recurring

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private Boolean isAvailable;

    private String note;

    private List<LocalTime> availableSlots; // aligne avec l’entité
    private List<LocalTime> bookedSlots;

    public static AvailabilityDto fromEntity(Availability a) {
        if (a == null)
            return null;
        return AvailabilityDto.builder()
                .id(a.getId())
                .barberId(a.getBarber() != null ? a.getBarber().getId() : null)
                .date(a.getDate())
                .dayOfWeek(a.getDayOfWeek())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .isAvailable(a.getIsAvailable())
                .note(a.getNote())
                .availableSlots(a.getAvailableSlots())
                .bookedSlots(a.getBookedSlots())
                .build();
    }

    /** DTO -> entity (sans lier le barber ici). */
    public static Availability toEntity(AvailabilityDto dto) {
        if (dto == null)
            return null;
        Availability a = new Availability();
        a.setId(dto.getId());
        a.setDate(dto.getDate());
        a.setDayOfWeek(dto.getDayOfWeek());
        a.setStartTime(dto.getStartTime());
        a.setEndTime(dto.getEndTime());
        a.setIsAvailable(dto.getIsAvailable());
        a.setNote(dto.getNote());
        a.setAvailableSlots(dto.getAvailableSlots());
        a.setBookedSlots(dto.getBookedSlots());
        return a;
    }

    // public Object getWindows() {
    // // TODO Auto-generated method stub
    // throw new UnsupportedOperationException("Unimplemented method 'getWindows'");
    // }
}
