package com.salahtech.BarberShop_Apis.reppsitories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.salahtech.BarberShop_Apis.models.GlobalSettings;

public interface GlobalSettingsRepository extends JpaRepository<GlobalSettings, Long> {
}
