package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.CreateWorkstationRequest;
import com.salahtech.BarberShop_Apis.Dtos.UpdateWorkstationRequest;
import com.salahtech.BarberShop_Apis.Dtos.WorkstationDto;
import com.salahtech.BarberShop_Apis.Enums.StationStatus;

import java.util.List;
import java.util.Map;

public interface WorkstationService {

    WorkstationDto create(CreateWorkstationRequest request);

    WorkstationDto update(Long id, UpdateWorkstationRequest request);

    WorkstationDto findById(Long id);

    List<WorkstationDto> findBySalon(Long salonId);

    List<WorkstationDto> findBySalonAndStatus(Long salonId, StationStatus status);

    WorkstationDto changeStatus(Long id, StationStatus status);

    WorkstationDto assignEmployee(Long id, Long employeeId);

    WorkstationDto releaseEmployee(Long id);

    /** Affecte un barbier au poste (modèle barber-centric). */
    WorkstationDto assignBarber(Long id, Long barberId);

    Map<StationStatus, Long> getStatusSummary(Long salonId);

    void delete(Long id);
}