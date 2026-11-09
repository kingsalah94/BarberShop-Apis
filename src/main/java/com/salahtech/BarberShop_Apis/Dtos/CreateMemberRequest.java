package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.SalonRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Création d'un sous-compte (manager / réceptionniste) par le propriétaire. */
@Data
public class CreateMemberRequest {

    @NotBlank(message = "Le prénom est requis")
    private String firstName;

    @NotBlank(message = "Le nom est requis")
    private String lastName;

    @Email(message = "Email invalide")
    @NotBlank(message = "L'email est requis")
    private String email;

    private String phone;

    @NotNull(message = "Le rôle est requis")
    private SalonRole role; // MANAGER ou RECEPTIONIST (pas OWNER)

    /** Optionnel : mot de passe initial. Si absent, un mot de passe temporaire est généré. */
    private String password;
}
