package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Config.TenantGuard;
import com.salahtech.BarberShop_Apis.Dtos.CreateWorkstationRequest;
import com.salahtech.BarberShop_Apis.Dtos.UpdateWorkstationRequest;
import com.salahtech.BarberShop_Apis.Dtos.WorkstationDto;
import com.salahtech.BarberShop_Apis.Enums.StationStatus;
import com.salahtech.BarberShop_Apis.Services.Interfaces.WorkstationService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Postes de travail du salon. Autorisation par membership :
 * lecture = tout membre (front-desk), écriture = OWNER + MANAGER (gestion du personnel).
 */
@RestController
@RequestMapping("/v1/workstations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Workstations", description = "Gestion des postes de travail du salon")
public class WorkstationController {

    private final WorkstationService workstationService;
    private final TenantGuard tenantGuard;

    @Operation(summary = "Créer un poste de travail")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkstationDto> create(@Valid @RequestBody CreateWorkstationRequest req,
                                                 @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageStaff(user, req.getSalonId());
        return ResponseEntity.status(HttpStatus.CREATED).body(workstationService.create(req));
    }

    @Operation(summary = "Mettre à jour un poste")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkstationDto> update(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateWorkstationRequest req,
                                                 @CurrentUser ApplicationUser user) {
        guardStaff(user, id);
        return ResponseEntity.ok(workstationService.update(id, req));
    }

    @Operation(summary = "Détails d'un poste")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkstationDto> getById(@PathVariable Long id,
                                                  @CurrentUser ApplicationUser user) {
        WorkstationDto dto = workstationService.findById(id);
        tenantGuard.assertCanFrontDesk(user, dto.getSalonId());
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Lister les postes d'un salon (option : filtrer par statut)")
    @GetMapping("/salon/{salonId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkstationDto>> list(@PathVariable Long salonId,
                                                     @RequestParam(required = false) StationStatus status,
                                                     @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        if (status != null) {
            return ResponseEntity.ok(workstationService.findBySalonAndStatus(salonId, status));
        }
        return ResponseEntity.ok(workstationService.findBySalon(salonId));
    }

    @Operation(summary = "Résumé du nombre de postes par statut")
    @GetMapping("/salon/{salonId}/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<StationStatus, Long>> summary(@PathVariable Long salonId,
                                                            @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        return ResponseEntity.ok(workstationService.getStatusSummary(salonId));
    }

    @Operation(summary = "Changer le statut d'un poste")
    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkstationDto> changeStatus(@PathVariable Long id,
                                                       @RequestParam StationStatus status,
                                                       @CurrentUser ApplicationUser user) {
        guardStaff(user, id);
        return ResponseEntity.ok(workstationService.changeStatus(id, status));
    }

    @Operation(summary = "Assigner un employé à un poste")
    @PostMapping("/{id}/assign/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkstationDto> assign(@PathVariable Long id, @PathVariable Long employeeId,
                                                 @CurrentUser ApplicationUser user) {
        guardFrontDesk(user, id); // attribution du titulaire = opération de front-desk (réception incluse)
        return ResponseEntity.ok(workstationService.assignEmployee(id, employeeId));
    }

    @Operation(summary = "Affecter un barbier au poste (modèle barber-centric)")
    @PostMapping("/{id}/assign-barber/{barberId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkstationDto> assignBarber(@PathVariable Long id, @PathVariable Long barberId,
                                                       @CurrentUser ApplicationUser user) {
        guardStaff(user, id);
        return ResponseEntity.ok(workstationService.assignBarber(id, barberId));
    }

    @Operation(summary = "Libérer le poste")
    @PostMapping("/{id}/release")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkstationDto> release(@PathVariable Long id,
                                                  @CurrentUser ApplicationUser user) {
        guardFrontDesk(user, id);
        return ResponseEntity.ok(workstationService.releaseEmployee(id));
    }

    @Operation(summary = "Supprimer un poste")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @CurrentUser ApplicationUser user) {
        guardStaff(user, id);
        workstationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Écriture/config (créer, modifier, supprimer) : OWNER + MANAGER sur le salon du poste. */
    private void guardStaff(ApplicationUser user, Long id) {
        WorkstationDto dto = workstationService.findById(id);
        tenantGuard.assertCanManageStaff(user, dto.getSalonId());
    }

    /** Opérations de front-desk (attribuer/libérer le titulaire) : tout membre, réception incluse. */
    private void guardFrontDesk(ApplicationUser user, Long id) {
        WorkstationDto dto = workstationService.findById(id);
        tenantGuard.assertCanFrontDesk(user, dto.getSalonId());
    }
}
