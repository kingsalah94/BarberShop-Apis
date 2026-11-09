package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.Enums.StationStatus;
import com.salahtech.BarberShop_Apis.models.Workstation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkstationRepository extends JpaRepository<Workstation, Long> {

    List<Workstation> findBySalon_IdOrderByDisplayOrderAscIdAsc(Long salonId);

    List<Workstation> findBySalon_IdAndStatus(Long salonId, StationStatus status);

    List<Workstation> findBySalon_IdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(Long salonId);

    long countBySalon_IdAndStatus(Long salonId, StationStatus status);

    long countBySalon_Id(Long salonId);
}
