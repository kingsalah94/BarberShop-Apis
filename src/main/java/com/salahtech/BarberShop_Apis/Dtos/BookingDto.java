package com.salahtech.BarberShop_Apis.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.models.Service;

/**
 * DTO for Booking entity.
 */
@Data
@Builder
public class BookingDto {

    private Long id;

    private ApplicationUserDto userDto;

    private BarberDto barberDto;

    private ServiceDto serviceDto;

    private LocalDateTime bookingDate;

    private BookingStatus status;

    private BigDecimal totalPrice;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


    public static BookingDto fromEntity(Booking booking) {
        return BookingDto.builder()
                .id(booking.getId())
                .userDto(ApplicationUserDto.fromEntity(booking.getUser()))
                .barberDto(BarberDto.fromEntity(booking.getBarber()))
                .serviceDto(ServiceDto.fromService(booking.getService()))
                .bookingDate(booking.getBookingDate())
                .status(booking.getStatus())
                .totalPrice(booking.getTotalPrice())
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    public static Booking toEntity(BookingDto dto) {

        if (dto == null) {
            return null;
        }

        Booking booking = new Booking();
        booking.setId(dto.id);
        booking.setUser(ApplicationUserDto.toEntity(dto.getUserDto())); // Assuming User has a constructor that accepts ID
        booking.setBarber(BarberDto.toEntity(dto.barberDto)); // Assuming Barber has a constructor that accepts ID
        booking.setService(ServiceDto.toEntity(dto.serviceDto)); // Assuming Service has a constructor that accepts ID
        booking.setBookingDate(dto.bookingDate);
        booking.setStatus(dto.status);
        booking.setTotalPrice(dto.totalPrice);
        booking.setCreatedAt(dto.createdAt);
        booking.setUpdatedAt(dto.updatedAt);
        return booking;
    }
    public void updateFromDto(BookingDto dto) {
        dto.bookingDate = dto.getBookingDate();
        dto.status = dto.getStatus();
        dto.totalPrice = dto.getTotalPrice();
        dto.notes = dto.getNotes();
        dto.updatedAt = LocalDateTime.now();
     } // Update the timestamp on update
}

