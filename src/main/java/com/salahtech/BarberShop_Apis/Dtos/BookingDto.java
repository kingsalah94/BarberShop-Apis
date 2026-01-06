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

    // --- Relations DTO complètes (optionnel pour le détail) ---
    private ApplicationUserDto clientUser;
    private BarberDto barberDto;
    private ServiceDto serviceDto;

    // --- IDs pour la création/mise à jour ---
    private Long clientId;

    @NotNull(message = "Barber ID is required")
    private Long barberId;

    @NotNull(message = "Service ID is required")
    private Long serviceId;

    // --- Données principales ---
    @NotNull(message = "Start time is required")
    // @Future(message = "Start time must be in the future") // Parfois gênant en
    // update, à voir
    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private BookingStatus status;
    private BigDecimal totalPrice;
    private String notes;

    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- Champs enrichis (Flattened) pour affichage facile dans les tableaux ---
    private String clientFirstName;
    private String clientLastName;
    private String barberName;
    private String salonName;
    private String serviceName;
    private Integer serviceDuration;
    private BigDecimal servicePrice;
    private String location;

    // ============================================================
    // 🛠️ MAPPING ENTITY -> DTO (LA CORRECTION EST ICI)
    // ============================================================
    public static BookingDto fromEntity(Booking b) {
        if (b == null)
            return null;

        // 1. Construction de base
        var dto = BookingDto.builder()
                .id(b.getId())
                .startAt(b.getStartAt())
                .endAt(b.getEndAt())
                .status(b.getStatus())
                .totalPrice(b.getTotalPrice())
                .notes(b.getNotes())
                .paymentMethod(b.getPaymentMethod())
                .paymentStatus(b.getPaymentStatus())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();

        // 2. Mapping Client
        if (b.getClient() != null) {
            dto.setClientId(b.getClient().getId());
            // ✅ CORRECTION : On remplit les noms pour l'affichage
            dto.setClientFirstName(b.getClient().getFirstName());
            dto.setClientLastName(b.getClient().getLastName());

            // Si le client n'a pas de nom propre mais utilise celui du User lié :

            if (b.getClient().getUser() != null) {
                if (dto.getClientFirstName() == null)
                    dto.setClientFirstName(b.getClient().getUser().getFirstName());
                if (dto.getClientLastName() == null)
                    dto.setClientLastName(b.getClient().getUser().getLastName());
            }

        }

        // 3. Mapping Barbier
        if (b.getBarber() != null) {
            dto.setBarberId(b.getBarber().getId());
            dto.setBarberDto(BarberDto.fromEntity(b.getBarber()));
            dto.setSalonName(b.getBarber().getSalonName());
            dto.setLocation(b.getBarber().getLocation());

            // ✅ CORRECTION : On remplit le nom du barbier
            // Vérifie si ton entité Barber a un champ 'name', sinon utilise
            // 'user.firstName'
            dto.setBarberName(b.getBarber().getName());

            // Fallback si name est null (au cas où)
            if (dto.getBarberName() == null && b.getBarber().getUser() != null) {
                dto.setBarberName(b.getBarber().getUser().getFirstName() + " " + b.getBarber().getUser().getLastName());
            }
        }

        // 4. Mapping Service (Prestation)
        // Attention : Vérifie si ton entité s'appelle 'getBarberService()' ou
        // 'getService()'
        if (b.getBarberService() != null) {
            dto.setServiceId(b.getBarberService().getId());
            dto.setServiceDto(ServiceDto.fromService(b.getBarberService()));

            // ✅ CORRECTION : On remplit les infos du service
            dto.setServiceName(b.getBarberService().getName());
            dto.setServiceDuration(b.getBarberService().getDuration());
            // dto.setServicePrice(b.getBarberService().getPrice()); // Optionnel
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
        b.setEndAt(dto.getEndAt());
        b.setStatus(dto.getStatus());
        b.setTotalPrice(dto.getTotalPrice());
        b.setNotes(dto.getNotes());
        b.setPaymentMethod(dto.getPaymentMethod());
        b.setPaymentStatus(dto.getPaymentStatus());
        return b;
    }
}