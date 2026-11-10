package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.SalonClientNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalonClientNoteRepository extends JpaRepository<SalonClientNote, Long> {

    Optional<SalonClientNote> findBySalon_IdAndClient_Id(Long salonId, Long clientId);

    @Query("SELECT n.client.id, n.note FROM SalonClientNote n WHERE n.salon.id = :salonId")
    List<Object[]> notesOfSalon(@Param("salonId") Long salonId);
}
