package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.AuditEventDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.OffsetDateTime;

public interface AuditService {
    Page<AuditEventDto> search(String action, Long userId, OffsetDateTime from, OffsetDateTime to, Pageable pageable);
}