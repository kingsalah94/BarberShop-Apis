package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.CreateEmployeeRequest;
import com.salahtech.BarberShop_Apis.Dtos.EmployeeDto;
import com.salahtech.BarberShop_Apis.Dtos.TimeOffDto;
import com.salahtech.BarberShop_Apis.Dtos.UpdateEmployeeRequest;
import com.salahtech.BarberShop_Apis.Enums.EmployeeRole;

import java.util.List;

public interface EmployeeService {

    EmployeeDto create(CreateEmployeeRequest request);

    /** Profil employé rattaché à un compte utilisateur (espace pro du prestataire). */
    EmployeeDto findByUserId(Long userId);

    /** Congés / absences d'un prestataire. */
    List<TimeOffDto> listTimeOff(Long employeeId);

    TimeOffDto addTimeOff(Long employeeId, TimeOffDto request);

    void deleteTimeOff(Long employeeId, Long timeOffId);

    EmployeeDto update(Long id, UpdateEmployeeRequest request);

    EmployeeDto findById(Long id);

    List<EmployeeDto> findAllBySalon(Long salonId);

    List<EmployeeDto> findActiveBySalon(Long salonId);

    List<EmployeeDto> findAvailableBySalon(Long salonId);

    List<EmployeeDto> findBySalonAndRole(Long salonId, EmployeeRole role);

    EmployeeDto setAvailability(Long id, boolean isAvailable);

    EmployeeDto setActive(Long id, boolean isActive);

    EmployeeDto assignSalon(Long id, Long salonId);

    EmployeeDto removeSalon(Long id, Long salonId);

    EmployeeDto addSkill(Long id, Long categoryId);

    EmployeeDto removeSkill(Long id, Long categoryId);

    /**
     * Provisionne (à la demande) un compte de connexion pour un employé existant
     * qui n'en a pas encore. Renvoie le DTO avec le mot de passe temporaire.
     */
    EmployeeDto provisionLogin(Long id, String password);

    void delete(Long id);
}