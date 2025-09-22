package com.salahtech.BarberShop_Apis.reppsitories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.salahtech.BarberShop_Apis.models.AuditEvent;

public interface AuditEventRepository
        extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {

}
