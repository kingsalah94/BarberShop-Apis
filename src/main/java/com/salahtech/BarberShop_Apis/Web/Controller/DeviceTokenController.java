package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Dtos.DeviceTokenDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;

@RestController
@RequestMapping("/v1/device-tokens")
@RequiredArgsConstructor
@Tag(name = "Device Tokens", description = "Gestion des tokens Push Notification (Admin)")
public class DeviceTokenController {

    private final DeviceTokenService service;

    @Operation(summary = "Rechercher des tokens")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DeviceTokenDto>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Set<String> types,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedBefore,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.search(userId, active, types, updatedBefore, pageable));
    }

    @Operation(summary = "Activer/Désactiver un token")
    @PostMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceTokenDto> activate(@PathVariable Long id, @RequestParam boolean value) {
        return ResponseEntity.ok(service.activate(id, value));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceTokenDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
}