package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Dtos.PermissionDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.PermissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "Permissions", description = "CRUD & Search des permissions")
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {

    private final PermissionService service;

    @GetMapping
    // @PreAuthorize("hasAuthority('permissions:read')")
    public ResponseEntity<Page<PermissionDto>> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(service.search(q, pageable));
    }

    @PostMapping
    // @PreAuthorize("hasAuthority('permissions:create')")
    public ResponseEntity<PermissionDto> create(@Valid @RequestBody PermissionDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    // @PreAuthorize("hasAuthority('permissions:update')")
    public ResponseEntity<PermissionDto> update(@PathVariable Long id, @RequestBody PermissionDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    // @PreAuthorize("hasAuthority('permissions:delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    // @PreAuthorize("hasAuthority('permissions:read')")
    public ResponseEntity<PermissionDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
