package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.EffectiveHoursDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonExceptionDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonHoursDto;

import java.time.LocalDate;
import java.util.List;

/** Horaires d'ouverture d'un salon : semaine type + exceptions ponctuelles. */
public interface SalonHoursService {

    /** Semaine type complète (7 jours, lundi→dimanche ; jours non configurés = fermés). */
    List<SalonHoursDto> getWeeklyHours(Long salonId);

    /** Remplace la semaine type par la liste fournie (upsert par jour). */
    List<SalonHoursDto> updateWeeklyHours(Long salonId, List<SalonHoursDto> hours);

    /** Exceptions du salon ({@code upcoming=true} → uniquement à partir d'aujourd'hui). */
    List<SalonExceptionDto> listExceptions(Long salonId, boolean upcoming);

    /** Crée (ou remplace pour cette date) une exception. */
    SalonExceptionDto createException(Long salonId, SalonExceptionDto req);

    /** Supprime une exception (en vérifiant qu'elle appartient bien au salon). */
    void deleteException(Long salonId, Long exceptionId);

    /** Horaires effectifs résolus pour une date (exception prioritaire, sinon semaine type). */
    EffectiveHoursDto getEffectiveHours(Long salonId, LocalDate date);
}
