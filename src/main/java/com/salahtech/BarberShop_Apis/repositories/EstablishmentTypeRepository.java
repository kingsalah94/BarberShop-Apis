package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.EstablishmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstablishmentTypeRepository extends JpaRepository<EstablishmentType, Long> {
    Optional<EstablishmentType> findByCode(String code);
}
