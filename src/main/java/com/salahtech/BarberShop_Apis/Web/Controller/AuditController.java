package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Dtos.AuditEventDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.AuditService;

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

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/v1/audit")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600) // On utilise * pour être cohérent avec les autres
@Tag(name = "Audit", description = "Logs et Traçabilité")
public class AuditController {

    private final AuditService auditService; // On passe par un service, pas le repo direct

    @Operation(summary = "Rechercher dans les logs")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // Sécurisé Admin only
    public ResponseEntity<Page<AuditEventDto>> search(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 20, sort = "ts", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(auditService.search(action, userId, from, to, pageable));
    }
}