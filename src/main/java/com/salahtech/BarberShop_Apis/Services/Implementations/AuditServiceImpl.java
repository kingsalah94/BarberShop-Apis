package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.AuditEventDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.AuditService;
import com.salahtech.BarberShop_Apis.models.AuditEvent;
import com.salahtech.BarberShop_Apis.reppsitories.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditServiceImpl implements AuditService {

    private final AuditEventRepository repository;

    @Override
    public Page<AuditEventDto> search(String action, Long userId, OffsetDateTime from, OffsetDateTime to,
            Pageable pageable) {

        // Construction dynamique de la requête SQL (Specification)
        Specification<AuditEvent> spec = Specification.where(null);

        if (action != null && !action.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("ts"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("ts"), to));
        }

        return repository.findAll(spec, pageable)
                .map(AuditEventDto::from);
    }
}