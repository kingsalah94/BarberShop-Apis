package com.salahtech.BarberShop_Apis.Dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateExceptionRequest {
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate exceptionDate;

    @NotBlank
    private String exceptionType; // "CLOSED" ou "CUSTOM"

    @NotBlank
    private String reason;

    private List<TimeSlotRequest> customTimeSlots;

    @Data
    public static class TimeSlotRequest {
        @NotBlank
        private String startAt; // "HH:mm"
        @NotBlank
        private String endAt;
    }
}