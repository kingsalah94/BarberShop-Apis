package com.salahtech.BarberShop_Apis.security.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.salahtech.BarberShop_Apis.security.models.SecuritySettings;

public interface SecuritySettingsRepository extends JpaRepository<SecuritySettings, Long> {
}
