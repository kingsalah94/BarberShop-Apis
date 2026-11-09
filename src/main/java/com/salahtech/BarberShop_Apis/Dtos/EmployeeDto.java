package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {
    private Long id;
    private Long userId;
    private Long barberId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private EmployeeRole role;
    private String roleDisplayName;
    private Set<Long> salonIds;
    private Long primarySalonId;
    // Boxed Boolean (non primitif) pour que Lombok génère getIsAvailable()/getIsActive()
    // et que Jackson sérialise les clés "isAvailable"/"isActive" attendues par le front
    // (un boolean primitif donnerait isAvailable()/isActive() => clés "available"/"active").
    private Boolean isAvailable;
    private Boolean isActive;
    private String calendarColor;
    private LocalDate hireDate;
    private String notes;
    private List<Long> skillIds;

    /** True si un compte de connexion est rattaché à cet employé. */
    private Boolean hasLogin;
    /** Mot de passe temporaire — renseigné uniquement à la création/provisioning du compte. */
    private String tempPassword;
}