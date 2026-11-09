package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.CreateWorkstationRequest;
import com.salahtech.BarberShop_Apis.Dtos.UpdateWorkstationRequest;
import com.salahtech.BarberShop_Apis.Dtos.WorkstationDto;
import com.salahtech.BarberShop_Apis.Enums.StationStatus;
import com.salahtech.BarberShop_Apis.Exceptions.BusinessException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.WorkstationService;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Employee;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.models.Workstation;
import com.salahtech.BarberShop_Apis.repositories.BarberRepository;
import com.salahtech.BarberShop_Apis.repositories.EmployeeRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonRepository;
import com.salahtech.BarberShop_Apis.repositories.WorkstationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WorkstationServiceImpl implements WorkstationService {

    private final WorkstationRepository workstationRepository;
    private final SalonRepository salonRepository;
    private final EmployeeRepository employeeRepository;
    private final BarberRepository barberRepository;

    @Override
    public WorkstationDto create(CreateWorkstationRequest req) {
        Salon salon = salonRepository.findById(req.getSalonId())
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable: " + req.getSalonId()));

        Workstation ws = Workstation.builder()
                .name(req.getName())
                .description(req.getDescription())
                .salon(salon)
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .status(StationStatus.FREE)
                .isActive(true)
                .build();

        Workstation saved = workstationRepository.save(ws);
        log.info("Workstation créée id={} salon={}", saved.getId(), salon.getId());
        return toDto(saved);
    }

    @Override
    public WorkstationDto update(Long id, UpdateWorkstationRequest req) {
        Workstation ws = workstationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workstation introuvable: " + id));

        if (req.getName() != null) ws.setName(req.getName());
        if (req.getDescription() != null) ws.setDescription(req.getDescription());
        if (req.getDisplayOrder() != null) ws.setDisplayOrder(req.getDisplayOrder());
        if (req.getIsActive() != null) ws.setActive(req.getIsActive());

        return toDto(workstationRepository.save(ws));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkstationDto findById(Long id) {
        return toDto(workstationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workstation introuvable: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkstationDto> findBySalon(Long salonId) {
        return workstationRepository.findBySalon_IdOrderByDisplayOrderAscIdAsc(salonId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkstationDto> findBySalonAndStatus(Long salonId, StationStatus status) {
        return workstationRepository.findBySalon_IdAndStatus(salonId, status).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public WorkstationDto changeStatus(Long id, StationStatus status) {
        Workstation ws = workstationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workstation introuvable: " + id));

        if (status == StationStatus.FREE) {
            ws.setCurrentEmployee(null);
        }
        ws.setStatus(status);
        log.info("Workstation {} -> status={}", id, status);
        return toDto(workstationRepository.save(ws));
    }

    @Override
    public WorkstationDto assignEmployee(Long id, Long employeeId) {
        Workstation ws = workstationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workstation introuvable: " + id));

        if (ws.getStatus() == StationStatus.OUT_OF_SERVICE) {
            throw new BusinessException("Ce poste est hors service");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable: " + employeeId));

        if (!employee.isActive()) {
            throw new BusinessException("Ce prestataire est inactif");
        }

        // Un prestataire ne tient qu'un seul poste : on le retire de tout autre poste du salon.
        if (ws.getSalon() != null) {
            for (Workstation other : workstationRepository.findBySalon_IdOrderByDisplayOrderAscIdAsc(ws.getSalon().getId())) {
                if (!other.getId().equals(id) && other.getCurrentEmployee() != null
                        && other.getCurrentEmployee().getId().equals(employeeId)) {
                    other.setCurrentEmployee(null);
                    if (other.getStatus() == StationStatus.OCCUPIED) other.setStatus(StationStatus.FREE);
                    workstationRepository.save(other);
                }
            }
        }

        // Titulaire du jour affecté. Le poste reste « libre » tant qu'aucun client n'y est en service
        // (le statut OCCUPIED est piloté par la file d'attente, pas par l'attribution du titulaire).
        ws.setCurrentEmployee(employee);
        return toDto(workstationRepository.save(ws));
    }

    @Override
    public WorkstationDto releaseEmployee(Long id) {
        Workstation ws = workstationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workstation introuvable: " + id));
        ws.setCurrentEmployee(null);
        ws.setCurrentBarber(null);
        if (ws.getStatus() == StationStatus.OCCUPIED) {
            ws.setStatus(StationStatus.FREE);
        }
        return toDto(workstationRepository.save(ws));
    }

    @Override
    public WorkstationDto assignBarber(Long id, Long barberId) {
        Workstation ws = workstationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workstation introuvable: " + id));

        if (ws.getStatus() == StationStatus.OUT_OF_SERVICE) {
            throw new BusinessException("Ce poste est hors service");
        }

        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barbier introuvable: " + barberId));

        ws.setCurrentBarber(barber);
        log.info("Workstation {} <- barbier {}", id, barberId);
        return toDto(workstationRepository.save(ws));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<StationStatus, Long> getStatusSummary(Long salonId) {
        Map<StationStatus, Long> summary = new EnumMap<>(StationStatus.class);
        for (StationStatus s : StationStatus.values()) {
            summary.put(s, workstationRepository.countBySalon_IdAndStatus(salonId, s));
        }
        return summary;
    }

    @Override
    public void delete(Long id) {
        if (!workstationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Workstation introuvable: " + id);
        }
        workstationRepository.deleteById(id);
    }

    private WorkstationDto toDto(Workstation w) {
        Employee e = w.getCurrentEmployee();
        Barber b = w.getCurrentBarber();
        return WorkstationDto.builder()
                .id(w.getId())
                .name(w.getName())
                .description(w.getDescription())
                .status(w.getStatus())
                .statusDisplayName(w.getStatus() != null ? w.getStatus().getDisplayName() : null)
                .salonId(w.getSalon() != null ? w.getSalon().getId() : null)
                .currentEmployeeId(e != null ? e.getId() : null)
                .currentEmployeeName(e != null ? (e.getFirstName() + " " + e.getLastName()) : null)
                .currentBarberId(b != null ? b.getId() : null)
                .currentBarberName(b != null ? b.getName() : null)
                .displayOrder(w.getDisplayOrder())
                .isActive(w.isActive())
                .build();
    }
}

