package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Enums.PaymentMethod;
import com.salahtech.BarberShop_Apis.Enums.PaymentStatus;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import lombok.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;

    // --- IDs ---
    private Long clientId;
    @NotNull
    private Long barberId;
    @NotNull
    private Long serviceId;

    // --- Données principales ---
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private BookingStatus status;
    private BigDecimal totalPrice;
    private String notes;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- Champs aplatis (Flattened) pour la sécurité Frontend ---
    private String clientFirstName;
    private String clientLastName;

    // ✅ LES NOUVEAUX CHAMPS (Envoyés directement à la racine)
    private String clientPhone;
    private String clientEmail;
    private String clientAvatar;

    private String barberName;
    private String salonName;
    private String serviceName;
    private Integer serviceDuration;
    private String location;

    // Objets complets (optionnels, gardés pour compatibilité)
    private BarberDto barberDto;
    private ServiceDto serviceDto;
    // On garde celui-ci au cas où, mais on ne l'utilise plus pour l'affichage
    // critique
    private ApplicationUserDto clientUser;

    // ============================================================
    // 🛠️ MAPPING ENTITY -> DTO (VERSION PRODUCTION)
    // ============================================================
    public static BookingDto fromEntity(Booking b) {
        if (b == null)
            return null;

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

        // --- MAPPING CLIENT ROBUSTE ---
        if (b.getClient() != null) {
            dto.setClientId(b.getClient().getId());

            // 1. On récupère les valeurs de base (Fiche Client)
            String fName = b.getClient().getFirstName();
            String lName = b.getClient().getLastName();
            String phone = b.getClient().getPhone();
            String email = b.getClient().getEmail();
            String avatar = b.getClient().getProfileImageUrl(); // Vérifie le nom exact dans ton entité Client

            // 2. On tente d'améliorer avec les données du User (Compte Application)
            try {
                if (b.getClient().getUser() != null) {
                    ApplicationUser u = b.getClient().getUser();

                    if (u.getFirstName() != null)
                        fName = u.getFirstName();
                    if (u.getLastName() != null)
                        lName = u.getLastName();

                    // Le User est prioritaire pour les contacts car c'est lui qui se connecte
                    if (u.getPhone() != null && !u.getPhone().isEmpty())
                        phone = u.getPhone();
                    if (u.getEmail() != null && !u.getEmail().isEmpty())
                        email = u.getEmail();
                    if (u.getProfileImageUrl() != null && !u.getProfileImageUrl().isEmpty())
                        avatar = u.getProfileImageUrl();
                }
            } catch (Exception e) {
                // Si LazyLoading exception, on garde les valeurs de l'étape 1
                System.out.println("Info: User non chargé pour le booking " + b.getId());
            }

            // 3. On injecte dans les champs plats
            dto.setClientFirstName(fName);
            dto.setClientLastName(lName);
            dto.setClientPhone(phone); // ✅
            dto.setClientEmail(email); // ✅
            dto.setClientAvatar(avatar); // ✅
        }

        // --- MAPPING BARBIER ---
        if (b.getBarber() != null) {
            dto.setBarberId(b.getBarber().getId());
            dto.setBarberDto(BarberDto.fromEntity(b.getBarber()));
            dto.setSalonName(b.getBarber().getSalonName());
            dto.setLocation(b.getBarber().getLocation());
            dto.setBarberName(b.getBarber().getName());

            if (dto.getBarberName() == null && b.getBarber().getUser() != null) {
                dto.setBarberName(b.getBarber().getUser().getFirstName() + " " + b.getBarber().getUser().getLastName());
            }
        }

        // --- MAPPING SERVICE ---
        if (b.getBarberService() != null) {
            dto.setServiceId(b.getBarberService().getId());
            dto.setServiceDto(ServiceDto.fromService(b.getBarberService()));
            dto.setServiceName(b.getBarberService().getName());
            dto.setServiceDuration(b.getBarberService().getDuration());
        }

        return dto;
    }
}