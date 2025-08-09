package com.salahtech.BarberShop_Apis.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.models.Booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for Booking entity.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingDto {

    private Long id;

    // DTOs complets pour les relations (pour les réponses)
    private ApplicationUserDto userDto;
    private BarberDto barberDto;
    private ServiceDto serviceDto;

    // IDs simples pour les requêtes de création/mise à jour
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Barber ID is required")
    private Long barberId;

    @NotNull(message = "Service ID is required")
    private Long serviceId;

    @NotNull(message = "Booking date is required")
    @Future(message = "Booking date must be in the future")
    private LocalDateTime bookingDate;

    private BookingStatus status;

    private BigDecimal totalPrice;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Champs pour la réponse enrichie
    private String clientFirstName;
    private String clientLastName;
    private String barberName;
    private String salonName;
    private String serviceName;
    private Integer serviceDuration;
    private BigDecimal servicePrice;
    private String location;

    /**
     * Convertit une entité Booking vers BookingDto avec toutes les informations
     */
    public static BookingDto fromEntity(Booking booking) {
        if (booking == null) {
            return null;
        }

        BookingDtoBuilder builder = BookingDto.builder()
                .id(booking.getId())
                .userId(booking.getUser() != null ? booking.getUser().getId() : null)
                .barberId(booking.getBarber() != null ? booking.getBarber().getId() : null)
                .serviceId(booking.getBarberService() != null ? booking.getBarberService().getId() : null)
                .bookingDate(booking.getBookingDate())
                .status(booking.getStatus())
                .totalPrice(booking.getTotalPrice())
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt());

        // Ajouter les DTOs complets si disponibles
        if (booking.getUser() != null) {
            builder.userDto(ApplicationUserDto.fromEntity(booking.getUser()))
                   .clientFirstName(booking.getUser().getFirstName())
                   .clientLastName(booking.getUser().getLastName());
        }

        if (booking.getBarber() != null) {
            builder.barberDto(BarberDto.fromEntity(booking.getBarber()))
                   .barberName(booking.getBarber().getName())
                   .salonName(booking.getBarber().getSalonName())
                   .location(booking.getBarber().getLocation());
        }

        if (booking.getBarberService() != null) {
            builder.serviceDto(ServiceDto.fromService(booking.getBarberService()))
                   .serviceName(booking.getBarberService().getName())
                   .serviceDuration(booking.getBarberService().getDuration())
                   .servicePrice(booking.getBarberService().getPrice());
        }

        return builder.build();
    }

    /**
     * Convertit BookingDto vers entité Booking (pour création/mise à jour)
     * Note: Les entités liées doivent être chargées séparément par ID
     */
    public static Booking toEntity(BookingDto dto) {
        if (dto == null) {
            return null;
        }

        Booking booking = new Booking();
        booking.setId(dto.getId());
        
        // Les entités liées seront définies par le service via les IDs
        // booking.setUser() - sera défini par le service
        // booking.setBarber() - sera défini par le service  
        // booking.setService() - sera défini par le service
        
        booking.setBookingDate(dto.getBookingDate());
        booking.setStatus(dto.getStatus());
        booking.setTotalPrice(dto.getTotalPrice());
        booking.setNotes(dto.getNotes());
        booking.setCreatedAt(dto.getCreatedAt());
        booking.setUpdatedAt(dto.getUpdatedAt());
        
        return booking;
    }

    /**
     * Met à jour les champs modifiables d'un Booking à partir du DTO
     */
    public void updateEntity(Booking booking) {
        if (booking == null) {
            return;
        }
        
        if (this.bookingDate != null) {
            booking.setBookingDate(this.bookingDate);
        }
        if (this.status != null) {
            booking.setStatus(this.status);
        }
        if (this.totalPrice != null) {
            booking.setTotalPrice(this.totalPrice);
        }
        if (this.notes != null) {
            booking.setNotes(this.notes);
        }
        
        booking.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Crée un DTO simple avec seulement les IDs (pour les requêtes)
     */
    public static BookingDto createRequest(Long userId, Long barberId, Long serviceId, 
                                         LocalDateTime bookingDate, String notes) {
        return BookingDto.builder()
                .userId(userId)
                .barberId(barberId)
                .serviceId(serviceId)
                .bookingDate(bookingDate)
                .notes(notes)
                .status(BookingStatus.PENDING) // Statut par défaut
                .build();
    }

    /**
     * Crée un DTO de mise à jour
     */
    public static BookingDto updateRequest(Long id, LocalDateTime bookingDate, 
                                         BookingStatus status, String notes) {
        return BookingDto.builder()
                .id(id)
                .bookingDate(bookingDate)
                .status(status)
                .notes(notes)
                .build();
    }
}