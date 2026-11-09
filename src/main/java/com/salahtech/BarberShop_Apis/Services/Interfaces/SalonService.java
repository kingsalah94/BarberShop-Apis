package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.BarberDto;
import com.salahtech.BarberShop_Apis.Dtos.CreateSalonRequest;
import com.salahtech.BarberShop_Apis.Dtos.SalonDto;
import com.salahtech.BarberShop_Apis.Dtos.UpdateSalonRequest;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

import java.util.List;

public interface SalonService {

    /** Crée un salon dont le propriétaire est {@code owner}. */
    SalonDto create(CreateSalonRequest req, ApplicationUser owner);

    /** Met à jour un salon (le contrôle d'appartenance est fait en amont). */
    SalonDto update(Long salonId, UpdateSalonRequest req);

    /** Définit le logo du salon (URL déjà stockée). */
    SalonDto setLogo(Long salonId, String url);

    /** Définit la photo de couverture du salon (URL déjà stockée). */
    SalonDto setCover(Long salonId, String url);

    SalonDto findById(Long salonId);

    /** Salons possédés par l'utilisateur courant. */
    List<SalonDto> findMine(ApplicationUser owner);

    /** Tous les salons (ADMIN). */
    List<SalonDto> findAll();

    void delete(Long salonId);

    /** Barbiers rattachés à un salon. */
    List<BarberDto> findBarbers(Long salonId);

    /** Rattache un barbier existant au salon. */
    void attachBarber(Long salonId, Long barberId);

    /** Détache un barbier du salon. */
    void detachBarber(Long salonId, Long barberId);
}
