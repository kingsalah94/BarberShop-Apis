package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.reppsitories.AuditEventRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.salahtech.BarberShop_Apis.Dtos.AuditEventDto;

import java.time.OffsetDateTime;
import com.salahtech.BarberShop_Apis.models.AuditEvent;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/audit")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
public class AuditController {

    private final AuditEventRepository repo;

    @GetMapping
    public ResponseEntity<Page<AuditEventDto>> search(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 20, sort = "ts", direction = Sort.Direction.DESC) Pageable pageable) {
        Specification<AuditEvent> spec = Specification.where(null);
        if (action != null && !action.isBlank())
            spec = spec.and((r, q, cb) -> cb.equal(r.get("action"), action));
        if (userId != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("userId"), userId));
        if (from != null)
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("ts"), from));
        if (to != null)
            spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("ts"), to));

        return ResponseEntity.ok(repo.findAll(spec, pageable).map(AuditEventDto::from));
    }
}
