package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.Salon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalonRepository extends JpaRepository<Salon, Long> {

    List<Salon> findByOwner_Id(Long ownerId);
}