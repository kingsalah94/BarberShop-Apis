package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Enums.PaymentMethod;
import com.salahtech.BarberShop_Apis.Enums.PaymentStatus;
import com.salahtech.BarberShop_Apis.models.Booking;
import lombok.*;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingDto {

    private Long id;

    // lecture complète
    private ApplicationUserDto clientUser; // optionnel: si tu veux exposer info user
    private BarberDto barberDto;
    private ServiceDto serviceDto;

    // pour création/màj (ids)
    private Long clientId; // lecture (server side), pas nécessaire en création
    @NotNull(message = "Barber ID is required")
    private Long barberId;
    @NotNull(message = "Service ID is required")
    private Long serviceId;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private BookingStatus status;
    private BigDecimal totalPrice;
    private String notes;

    // ...
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // enrichissements
    private String clientFirstName;
    private String clientLastName;
    private String barberName; // si tu as un champ name côté Barber
    private String salonName;
    private String serviceName;
    private Integer serviceDuration;
    private BigDecimal servicePrice;
    private String location;

    public static BookingDto fromEntity(Booking b) {
        if (b == null)
            return null;

        var dto = BookingDto.builder()
                .id(b.getId())
                .clientId(b.getClient() != null ? b.getClient().getId() : null)
                .barberId(b.getBarber() != null ? b.getBarber().getId() : null)
                .serviceId(b.getBarberService() != null ? b.getBarberService().getId() : null)
                .startAt(b.getStartAt())
                .endAt(b.getEndAt())
                .status(b.getStatus())
                .totalPrice(b.getTotalPrice())
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();

        if (b.getBarber() != null) {
            dto.setBarberDto(BarberDto.fromEntity(b.getBarber()));
            dto.setSalonName(b.getBarber().getSalonName());
            dto.setLocation(b.getBarber().getLocation());
        }
        if (b.getBarberService() != null) {
            dto.setServiceDto(ServiceDto.fromService(b.getBarberService()));
            dto.setServiceName(b.getBarberService().getName());
            dto.setServiceDuration(b.getBarberService().getDuration());
            dto.setServicePrice(b.getBarberService().getPrice());
        }
        if (b.getClient() != null && b.getClient().getUser() != null) {
            dto.setClientFirstName(b.getClient().getFirstName());
            dto.setClientLastName(b.getClient().getLastName());
        }
        return dto;
    }

    /** DTO -> entity (relations chargées par le service). */
    public static Booking toEntity(BookingDto dto) {
        if (dto == null)
            return null;
        Booking b = new Booking();
        b.setId(dto.getId());
        b.setStartAt(dto.getStartAt());
        b.setEndAt(dto.getEndAt()); // peut être calculé par service si null
        b.setStatus(dto.getStatus());
        b.setTotalPrice(dto.getTotalPrice());
        b.setNotes(dto.getNotes());
        return b;
    }
}
