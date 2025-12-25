package com.salahtech.BarberShop_Apis.Dtos;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateExceptionRequest {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate exceptionDate;
    private String exceptionType;
    private String reason;
    private List<CreateExceptionRequest.TimeSlotRequest> customTimeSlots;
}