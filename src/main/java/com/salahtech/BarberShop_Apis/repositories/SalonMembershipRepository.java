package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.SalonMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalonMembershipRepository extends JpaRepository<SalonMembership, Long> {

    List<SalonMembership> findByUser_Id(Long userId);

    List<SalonMembership> findBySalon_Id(Long salonId);

    Optional<SalonMembership> findByUser_IdAndSalon_Id(Long userId, Long salonId);

    boolean existsByUser_IdAndSalon_Id(Long userId, Long salonId);
}
