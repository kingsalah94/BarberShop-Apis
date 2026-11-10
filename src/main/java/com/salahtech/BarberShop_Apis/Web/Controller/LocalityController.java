package com.salahtech.BarberShop_Apis.Web.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Config.TenantGuard;
import com.salahtech.BarberShop_Apis.Dtos.LocalityDto;
import com.salahtech.BarberShop_Apis.Dtos.LocalityRefDto;
import com.salahtech.BarberShop_Apis.Dtos.LocalityStatDto;
import com.salahtech.BarberShop_Apis.Enums.LocalityType;
import com.salahtech.BarberShop_Apis.Services.Implementations.LocalityService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Référentiel géographique Pays › Région › Ville › Quartier. Lecture publique, ajout connecté, gestion admin. */
@RestController
@RequestMapping("/v1/localities")
@RequiredArgsConstructor
@Tag(name = "Localités", description = "Pays, régions, villes et quartiers")
public class LocalityController {

    private final LocalityService service;
    private final TenantGuard tenantGuard;

    @Operation(summary = "Enfants d'une localité (pays si parentId absent)")
    @GetMapping
    public ResponseEntity<List<LocalityDto>> children(@RequestParam(required = false) Long parentId) {
        return ResponseEntity.ok(service.children(parentId));
    }

    @Operation(summary = "Chemin pays → localité")
    @GetMapping("/{id}/path")
    public ResponseEntity<List<LocalityRefDto>> path(@PathVariable Long id) {
        return ResponseEntity.ok(service.path(id));
    }

    @Operation(summary = "Ajouter une ville ou un quartier manquant (admin : tout niveau)")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LocalityDto> create(@RequestBody Map<String, Object> body, @CurrentUser ApplicationUser user) {
        Long parentId = body.get("parentId") == null ? null : Long.valueOf(body.get("parentId").toString());
        String name = body.get("name") == null ? null : body.get("name").toString();
        return ResponseEntity.ok(service.create(parentId, name, tenantGuard.isAdmin(user)));
    }

    @Operation(summary = "Arbre complet avec compteurs (admin)")
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<LocalityDto>> all() {
        return ResponseEntity.ok(service.all());
    }

    @Operation(summary = "Statistiques par zone (admin) : salons, clients, réservations")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<LocalityStatDto>> stats(@RequestParam(defaultValue = "CITY") LocalityType level) {
        return ResponseEntity.ok(service.stats(level));
    }

    @Operation(summary = "Renommer / activer / valider une localité (admin)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<LocalityDto> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.update(id,
                body.get("name") == null ? null : body.get("name").toString(),
                body.get("active") == null ? null : Boolean.valueOf(body.get("active").toString()),
                body.get("suggested") == null ? null : Boolean.valueOf(body.get("suggested").toString())));
    }

    @Operation(summary = "Supprimer (si inutilisée) sinon désactiver (admin)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Boolean>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("deleted", service.deleteOrDeactivate(id)));
    }
}
