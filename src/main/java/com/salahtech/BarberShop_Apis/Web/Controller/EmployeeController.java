package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Config.TenantGuard;
import com.salahtech.BarberShop_Apis.Dtos.CreateEmployeeRequest;
import com.salahtech.BarberShop_Apis.Dtos.EmployeeDto;
import com.salahtech.BarberShop_Apis.Dtos.TimeOffDto;
import com.salahtech.BarberShop_Apis.Dtos.UpdateEmployeeRequest;
import com.salahtech.BarberShop_Apis.Enums.EmployeeRole;
import com.salahtech.BarberShop_Apis.Services.Interfaces.EmployeeService;
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

/**
 * Personnel interne du salon. Autorisation par membership :
 * gestion (CRUD) = OWNER + MANAGER ; consultation + présence = tout membre (front-desk).
 */
@RestController
@RequestMapping("/v1/employees")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Employees", description = "Gestion interne du personnel du salon")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final TenantGuard tenantGuard;

    @Operation(summary = "Créer un employé")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeDto> create(@Valid @RequestBody CreateEmployeeRequest request,
                                              @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageStaff(user, request.getPrimarySalonId());
        if (request.getSalonIds() != null) {
            request.getSalonIds().forEach(sid -> tenantGuard.assertCanManageStaff(user, sid));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(request));
    }

    @Operation(summary = "Mettre à jour un employé")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeDto> update(@PathVariable Long id,
                                              @Valid @RequestBody UpdateEmployeeRequest request,
                                              @CurrentUser ApplicationUser user) {
        guardStaff(user, id);
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @Operation(summary = "Détails d'un employé")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeDto> getById(@PathVariable Long id,
                                               @CurrentUser ApplicationUser user) {
        EmployeeDto dto = employeeService.findById(id);
        tenantGuard.assertCanFrontDesk(user, primarySalon(dto));
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Mon profil prestataire (utilisateur connecté)")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeDto> me(@CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(employeeService.findByUserId(user.getId()));
    }

    // ----- Congés / absences (Disponibilités) -----

    @Operation(summary = "Lister les congés d'un prestataire")
    @GetMapping("/{id}/timeoff")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TimeOffDto>> listTimeOff(@PathVariable Long id,
                                                        @CurrentUser ApplicationUser user) {
        guardSelfOrStaff(user, id);
        return ResponseEntity.ok(employeeService.listTimeOff(id));
    }

    @Operation(summary = "Ajouter un congé / une absence")
    @PostMapping("/{id}/timeoff")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TimeOffDto> addTimeOff(@PathVariable Long id,
                                                 @RequestBody TimeOffDto request,
                                                 @CurrentUser ApplicationUser user) {
        guardSelfOrStaff(user, id);
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.addTimeOff(id, request));
    }

    @Operation(summary = "Supprimer un congé")
    @DeleteMapping("/{id}/timeoff/{timeOffId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteTimeOff(@PathVariable Long id, @PathVariable Long timeOffId,
                                              @CurrentUser ApplicationUser user) {
        guardSelfOrStaff(user, id);
        employeeService.deleteTimeOff(id, timeOffId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lister tous les employés d'un salon")
    @GetMapping("/salon/{salonId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EmployeeDto>> listBySalon(
            @PathVariable Long salonId,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Boolean availableOnly,
            @RequestParam(required = false) EmployeeRole role,
            @CurrentUser ApplicationUser user) {

        tenantGuard.assertCanFrontDesk(user, salonId);
        if (Boolean.TRUE.equals(availableOnly)) {
            return ResponseEntity.ok(employeeService.findAvailableBySalon(salonId));
        }
        if (role != null) {
            return ResponseEntity.ok(employeeService.findBySalonAndRole(salonId, role));
        }
        if (Boolean.TRUE.equals(activeOnly)) {
            return ResponseEntity.ok(employeeService.findActiveBySalon(salonId));
        }
        return ResponseEntity.ok(employeeService.findAllBySalon(salonId));
    }

    @Operation(summary = "Marquer présent / absent")
    @PatchMapping("/{id}/availability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeDto> setAvailability(@PathVariable Long id,
                                                       @RequestParam boolean isAvailable,
                                                       @CurrentUser ApplicationUser user) {
        // Présence du jour = opération de front-desk (réceptionniste inclus).
        tenantGuard.assertCanFrontDesk(user, primarySalon(employeeService.findById(id)));
        return ResponseEntity.ok(employeeService.setAvailability(id, isAvailable));
    }

    @Operation(summary = "Activer / désactiver un employé")
    @PatchMapping("/{id}/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeDto> setActive(@PathVariable Long id,
                                                 @RequestParam boolean isActive,
                                                 @CurrentUser ApplicationUser user) {
        guardStaff(user, id);
        return ResponseEntity.ok(employeeService.setActive(id, isActive));
    }

    @Operation(summary = "Assigner l'employé à un salon supplémentaire")
    @PostMapping("/{id}/salons/{salonId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeDto> assignSalon(@PathVariable Long id, @PathVariable Long salonId,
                                                   @CurrentUser ApplicationUser user) {
        guardStaff(user, id);
        tenantGuard.assertCanManageStaff(user, salonId); // le salon cible doit aussi être à soi
        return ResponseEntity.ok(employeeService.assignSalon(id, salonId));
    }

    @Operation(summary = "Retirer un salon de l'employé")
    @DeleteMapping("/{id}/salons/{salonId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeDto> removeSalon(@PathVariable Long id, @PathVariable Long salonId,
                                                   @CurrentUser ApplicationUser user) {
        guardStaff(user, id);
        tenantGuard.assertCanManageStaff(user, salonId);
        return ResponseEntity.ok(employeeService.removeSalon(id, salonId));
    }

    @Operation(summary = "Ajouter une compétence (catégorie de service)")
    @PostMapping("/{id}/skills/{categoryId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeDto> addSkill(@PathVariable Long id, @PathVariable Long categoryId,
                                                @CurrentUser ApplicationUser user) {
        guardStaff(user, id);
        return ResponseEntity.ok(employeeService.addSkill(id, categoryId));
    }

    @Operation(summary = "Retirer une compétence")
    @DeleteMapping("/{id}/skills/{categoryId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeDto> removeSkill(@PathVariable Long id, @PathVariable Long categoryId,
                                                   @CurrentUser ApplicationUser user) {
        guardStaff(user, id);
        return ResponseEntity.ok(employeeService.removeSkill(id, categoryId));
    }

    @Operation(summary = "Donner un accès de connexion (espace pro) à un prestataire existant")
    @PostMapping("/{id}/login")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeDto> provisionLogin(@PathVariable Long id,
                                                      @RequestParam(required = false) String password,
                                                      @CurrentUser ApplicationUser user) {
        guardStaff(user, id);
        return ResponseEntity.ok(employeeService.provisionLogin(id, password));
    }

    @Operation(summary = "Supprimer un employé")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @CurrentUser ApplicationUser user) {
        guardStaff(user, id);
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Salon de référence d'un employé (principal, sinon premier rattachement). */
    private Long primarySalon(EmployeeDto e) {
        if (e.getPrimarySalonId() != null) return e.getPrimarySalonId();
        if (e.getSalonIds() != null && !e.getSalonIds().isEmpty()) return e.getSalonIds().iterator().next();
        return null;
    }

    /** Écriture/gestion : OWNER + MANAGER sur le salon de l'employé. */
    private void guardStaff(ApplicationUser user, Long id) {
        tenantGuard.assertCanManageStaff(user, primarySalon(employeeService.findById(id)));
    }

    /** Le prestataire lui-même (son propre profil) OU la gestion (OWNER/MANAGER). */
    private void guardSelfOrStaff(ApplicationUser user, Long id) {
        EmployeeDto e = employeeService.findById(id);
        if (e.getUserId() != null && e.getUserId().equals(user.getId())) return;
        tenantGuard.assertCanManageStaff(user, primarySalon(e));
    }
}
