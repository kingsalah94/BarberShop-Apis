package com.salahtech.BarberShop_Apis.reppsitories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.salahtech.BarberShop_Apis.models.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    Optional<Permission> findByName(String name);
    
    Boolean existsByName(String name);
}
