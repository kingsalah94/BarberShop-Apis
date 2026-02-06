package com.salahtech.BarberShop_Apis.Dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus; // Assure-toi que cet import est bon
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Client;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String lastName;

    @Email(message = "Format d'email invalide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    // ⚠️ Note: Ce regex est pour la France (+33). Pour l'international ou le Niger,
    // tu peux le simplifier.
    // Ex générique: "^\\+?[0-9]{8,15}$", fr "^(\\+33|0)[1-9]([0-9]{8})$"
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Format de numéro de téléphone invalide")
    private String phone;

    // ✅ CORRECTION PRINCIPALE : Le mot de passe est présent ici
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String profileImageUrl;
    private String preferences;
    private String notes;
    private Boolean active; // Ajouté pour correspondre à ton frontend

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Données enrichies (lecture seule)
    private ApplicationUserDto userDto;
    private Integer totalBookings;
    private Integer completedBookings;
    private LocalDateTime lastBookingDate;
    private String fullName;

    // ==========================================
    // 🔄 MAPPING ENTITY -> DTO
    // ==========================================
    public static ClientDto fromEntity(Client client) {
        if (client == null)
            return null;

        // On récupère les infos de l'User lié
        ApplicationUser user = client.getUser();

        return ClientDto.builder()
                .id(client.getId())
                .userId(user != null ? user.getId() : null)
                // On prend les infos de base depuis l'User (Authentification)
                .firstName(user != null ? user.getFirstName() : null)
                .lastName(user != null ? user.getLastName() : null)
                .email(user != null ? user.getEmail() : null)
                .phone(user != null ? user.getPhone() : null)
                .profileImageUrl(user != null ? user.getProfileImageUrl() : null)
                .active(user != null ? user.isEnabled() : true)

                // On prend les infos métier depuis le Client
                .preferences(client.getPreferences())
                .notes(client.getNotes())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())

                // Calculs complexes (Bookings)
                .totalBookings(client.getBookings() != null ? client.getBookings().size() : 0)
                .completedBookings(client.getBookings() != null ? (int) client.getBookings().stream()
                        .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                        .count() : 0)
                .lastBookingDate(
                        client.getBookings() != null && !client.getBookings().isEmpty() ? client.getBookings().stream()
                                .map(b -> b.getStartAt() != null ? b.getStartAt() : b.getBookingDate()) // Gère les deux
                                                                                                        // cas de date
                                .filter(java.util.Objects::nonNull)
                                .max(LocalDateTime::compareTo)
                                .orElse(null) : null)
                .fullName(client.getFullName()) // Supposant que getFullName existe dans Client ou User
                .build();
    }

    // ==========================================
    // 🔄 MAPPING DTO -> ENTITY
    // ==========================================
    // Note : Généralement on utilise le Service pour ça car il faut créer l'User ET
    // le Client
    public static Client toEntity(ClientDto dto) {
        if (dto == null)
            return null;

        Client client = new Client();
        client.setId(dto.getId());

        // Ces champs seront transférés à l'User dans le Service
        // Mais on peut les stocker temporairement si ton entité Client a des champs
        // transitoires
        client.setPreferences(dto.getPreferences());
        client.setNotes(dto.getNotes());

        return client;
    }
}