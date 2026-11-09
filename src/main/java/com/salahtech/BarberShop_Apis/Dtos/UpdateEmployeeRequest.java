package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.EmployeeRole;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
public class UpdateEmployeeRequest {
    private String firstName;
    private String lastName;

    @Email(message = "Email invalide")
    private String email;

    private String phoneNumber;
    private EmployeeRole role;
    private Set<Long> salonIds;
    private Long primarySalonId;
    private Boolean isAvailable;
    private Boolean isActive;
    private String calendarColor;
    private LocalDate hireDate;
    private String notes;
    private List<Long> skillIds;
}