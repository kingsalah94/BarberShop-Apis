package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Services.Interfaces.SecuritySettingsService;
import com.salahtech.BarberShop_Apis.security.Dtos.SecuritySettingsDto;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/security")
@RequiredArgsConstructor
@Tag(name = "Security Settings", description = "Lecture & Mise à jour des paramètres de sécurité")
@PreAuthorize("hasRole('ADMIN')")
public class SecuritySettingsController {

    private final SecuritySettingsService service;

    @GetMapping("/settings")
    // @PreAuthorize("hasAuthority('security:read')")
    public ResponseEntity<SecuritySettingsDto> get() {
        return ResponseEntity.ok(service.get());
    }

    @PutMapping("/settings")
    // @PreAuthorize("hasAuthority('security:update')")
    public ResponseEntity<SecuritySettingsDto> update(@Valid @RequestBody SecuritySettingsDto dto) {
        return ResponseEntity.ok(service.update(dto));
    }
}
