package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.SalonFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SalonFavoriteRepository extends JpaRepository<SalonFavorite, Long> {

    boolean existsByClient_IdAndSalon_Id(Long clientId, Long salonId);

    @Query("select f.salon.id from SalonFavorite f where f.client.id = :clientId order by f.createdAt desc")
    List<Long> findSalonIdsByClientId(@Param("clientId") Long clientId);

    @Transactional
    long deleteByClient_IdAndSalon_Id(Long clientId, Long salonId);

    long countByClient_Id(Long clientId);
}
