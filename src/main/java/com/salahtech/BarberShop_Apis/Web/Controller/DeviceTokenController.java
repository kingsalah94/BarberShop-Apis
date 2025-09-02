package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Dtos.DeviceTokenDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.DeviceTokenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
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
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Device Tokens", description = "CRUD & Search des tokens push")
public class DeviceTokenController {

    private final DeviceTokenService service;

    @GetMapping
    @PreAuthorize("hasAuthority('devicetokens:read')")
    public ResponseEntity<Page<DeviceTokenDto>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Set<String> types,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedBefore,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.search(userId, active, types, updatedBefore, pageable));
    }

    @PostMapping("/register")
    @PreAuthorize("hasAuthority('devicetokens:create')")
    public ResponseEntity<DeviceTokenDto> register(@RequestBody DeviceTokenDto dto) {
        return ResponseEntity.ok(service.register(dto));
    }

    @PostMapping("/{id}/active")
    @PreAuthorize("hasAuthority('devicetokens:update')")
    public ResponseEntity<DeviceTokenDto> activate(@PathVariable Long id, @RequestParam boolean value) {
        return ResponseEntity.ok(service.activate(id, value));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('devicetokens:delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('devicetokens:read')")
    public ResponseEntity<DeviceTokenDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
