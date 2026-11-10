package com.salahtech.BarberShop_Apis.Web.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.LocalityRefDto;
import com.salahtech.BarberShop_Apis.Services.Implementations.LocalityService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.repositories.ApplicationUserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Zone de l'utilisateur connecté (ville / quartier) : filtre par défaut du catalogue + statistiques. */
@RestController
@RequestMapping("/v1/me/locality")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Ma localité")
public class MyLocalityController {

    private final LocalityService localities;
    private final ApplicationUserRepository users;

    @Operation(summary = "Ma zone (chemin pays → zone ; liste vide si non renseignée)")
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<LocalityRefDto>> mine(@CurrentUser ApplicationUser user) {
        ApplicationUser u = users.findById(user.getId()).orElseThrow();
        return ResponseEntity.ok(u.getLocality() == null ? List.of() : localities.pathOf(u.getLocality()));
    }

    @Operation(summary = "Définir (ou effacer avec localityId null) ma zone")
    @PutMapping
    @Transactional
    public ResponseEntity<List<LocalityRefDto>> set(@RequestBody Map<String, Object> body, @CurrentUser ApplicationUser user) {
        ApplicationUser u = users.findById(user.getId()).orElseThrow();
        Object raw = body.get("localityId");
        u.setLocality(raw == null ? null : localities.find(Long.valueOf(raw.toString())));
        users.save(u);
        return ResponseEntity.ok(u.getLocality() == null ? List.of() : localities.pathOf(u.getLocality()));
    }
}
