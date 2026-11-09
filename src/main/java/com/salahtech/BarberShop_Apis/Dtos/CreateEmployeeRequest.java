package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.EmployeeRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
public class CreateEmployeeRequest {

    @NotBlank(message = "Le prénom est requis")
    private String firstName;

    @NotBlank(message = "Le nom est requis")
    private String lastName;

    @Email(message = "Email invalide")
    private String email;

    private String phoneNumber;

    @NotNull(message = "Le rôle est requis")
    private EmployeeRole role;

    @NotEmpty(message = "Au moins un salon doit être assigné")
    private Set<Long> salonIds;

    @NotNull(message = "Le salon principal est requis")
    private Long primarySalonId;

    private Long userId;

    private Long barberId;

    private String calendarColor;

    private LocalDate hireDate;

    private String notes;

    private List<Long> skillIds;

    /** Si true, provisionne un compte de connexion (espace pro) pour ce prestataire. */
    private Boolean createLogin;

    /** Mot de passe initial (optionnel ; généré si absent). Requiert un email. */
    private String loginPassword;
}