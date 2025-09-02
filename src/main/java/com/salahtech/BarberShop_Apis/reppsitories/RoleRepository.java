package com.salahtech.BarberShop_Apis.reppsitories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.salahtech.BarberShop_Apis.models.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

    // Optional<Role> findByName(String name);

    Boolean existsByName(String name);

    Optional<Role> findByName(String name);
}
