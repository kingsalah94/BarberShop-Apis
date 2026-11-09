package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.SalonPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalonPostRepository extends JpaRepository<SalonPost, Long> {
    List<SalonPost> findBySalon_IdOrderByCreatedAtDesc(Long salonId);
    List<SalonPost> findTop30ByOrderByCreatedAtDesc();
}
