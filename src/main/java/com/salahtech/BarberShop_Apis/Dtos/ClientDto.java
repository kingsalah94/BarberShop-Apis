package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.models.Client;
import lombok.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDto {

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

    @Pattern(regexp = "^(\\+33|0)[1-9]([0-9]{8})$", message = "Format de numéro de téléphone invalide")
    private String phone;

    private String preferences;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Données enrichies (lecture seule)
    private ApplicationUserDto userDto;
    private Integer totalBookings;
    private Integer completedBookings;
    private LocalDateTime lastBookingDate;
    private String fullName;

    public static ClientDto fromEntity(Client client) {
        if (client == null)
            return null;

        return ClientDto.builder()
                .id(client.getId())
                .userId(client.getUser() != null ? client.getUser().getId() : null)
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .email(client.getEmail())
                .phone(client.getPhone())
                .preferences(client.getPreferences())
                .notes(client.getNotes())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .userDto(client.getUser() != null ? ApplicationUserDto.fromEntity(client.getUser()) : null)
                .totalBookings(client.getBookings() != null ? client.getBookings().size() : 0)
                .completedBookings(client.getBookings() != null ? (int) client.getBookings().stream()
                        .filter(b -> b.getStatus() == com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED)
                        .count() : 0)
                .lastBookingDate(
                        client.getBookings() != null && !client.getBookings().isEmpty() ? client.getBookings().stream()
                                .map(b -> b.getStartAt() != null ? b.getStartAt() : b.getBookingDate())
                                .filter(d -> d != null)
                                .max(LocalDateTime::compareTo)
                                .orElse(null) : null)
                .fullName(client.getFullName())
                .build();
    }

    public static Client toEntity(ClientDto dto) {
        if (dto == null)
            return null;

        Client client = new Client();
        client.setId(dto.getId());
        client.setFirstName(dto.getFirstName());
        client.setLastName(dto.getLastName());
        client.setEmail(dto.getEmail());
        client.setPhone(dto.getPhone());
        client.setPreferences(dto.getPreferences());
        client.setNotes(dto.getNotes());
        // user et dates gérés par le service
        return client;
    }
}