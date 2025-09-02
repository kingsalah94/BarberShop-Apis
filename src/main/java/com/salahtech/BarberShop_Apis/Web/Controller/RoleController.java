package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Dtos.RoleDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.RoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/roles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Roles", description = "CRUD & Search des rôles")
public class RoleController {

    private final RoleService service;

    @GetMapping
    // @PreAuthorize("hasAuthority('roles:read')")
    public ResponseEntity<Page<RoleDto>> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(service.search(q, pageable));
    }

    @PostMapping
    // @PreAuthorize("hasAuthority('roles:create')")
    public ResponseEntity<RoleDto> create(@Valid @RequestBody RoleDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    // @PreAuthorize("hasAuthority('roles:update')")
    public ResponseEntity<RoleDto> update(@PathVariable Long id, @RequestBody RoleDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    // @PreAuthorize("hasAuthority('roles:delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    // @PreAuthorize("hasAuthority('roles:read')")
    public ResponseEntity<RoleDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
