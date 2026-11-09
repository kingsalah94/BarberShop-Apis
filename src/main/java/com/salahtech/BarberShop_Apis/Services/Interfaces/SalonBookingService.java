package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.CreateSalonBookingRequest;
import com.salahtech.BarberShop_Apis.Dtos.SalonBookingDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonStatsDto;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

import java.time.LocalDate;
import java.util.List;

/** Réservation orientée salon : créneaux, création, suivi, confirmation. */
public interface SalonBookingService {

    /** Créneaux disponibles ("HH:mm") pour un salon + service + date. */
    default List<String> availableSlots(Long salonId, Long serviceId, LocalDate date) {
        return availableSlots(salonId, serviceId, date, null);
    }

    /** Créneaux libres ; avec {@code employeeId}, uniquement ceux où ce prestataire est libre. */
    List<String> availableSlots(Long salonId, Long serviceId, LocalDate date, Long employeeId);

    /** Prestataires que le client peut choisir (barbiers actifs du salon). */
    List<com.salahtech.BarberShop_Apis.Dtos.SalonStaffDto> bookableStaff(Long salonId);

    /** Prochaines réservations actives, tous salons confondus (console admin). */
    List<com.salahtech.BarberShop_Apis.Dtos.SalonBookingDto> upcomingPlatform();

    /** Crée une réservation (client connecté). */
    SalonBookingDto create(Long salonId, CreateSalonBookingRequest req, ApplicationUser user);

    /** Réservations d'un salon pour une date (vue staff). */
    List<SalonBookingDto> listForSalon(Long salonId, LocalDate date);

    /**
     * Réservations d'un salon pour une date, éventuellement limitées à un employé.
     * Si {@code employeeId} est non nul, ne renvoie que les RDV affectés à ce prestataire.
     */
    List<SalonBookingDto> listForSalon(Long salonId, LocalDate date, Long employeeId);

    /** Réservations du client connecté. */
    List<SalonBookingDto> listMine(ApplicationUser user);

    /** Affecte (ou réaffecte) un prestataire à une réservation, sans changer le statut. */
    SalonBookingDto assignEmployee(Long salonId, Long bookingId, Long employeeId);

    /** Confirme une réservation et l'affecte éventuellement à un poste / prestataire. */
    SalonBookingDto confirm(Long salonId, Long bookingId, Long workstationId, Long employeeId);

    /** Le client est arrivé : confirme si besoin, affecte poste/prestataire, ajoute à la file. */
    SalonBookingDto checkIn(Long salonId, Long bookingId, Long workstationId, Long employeeId);

    /** Annule une réservation côté staff (salon). */
    SalonBookingDto cancel(Long salonId, Long bookingId, ApplicationUser user);

    /** Annule une réservation côté client (doit être le propriétaire). */
    SalonBookingDto cancelMine(Long bookingId, ApplicationUser user);

    /** Laisse un avis sur une réservation terminée (client propriétaire, une seule fois). */
    SalonBookingDto review(Long bookingId,
            com.salahtech.BarberShop_Apis.Dtos.CreateSalonReviewRequest req, ApplicationUser user);

    /** Statistiques agrégées des réservations d'un salon sur une période. */
    SalonStatsDto getStats(Long salonId, LocalDate from, LocalDate to);

    /** Statistiques limitées à un prestataire (si employeeId non nul). */
    SalonStatsDto getStats(Long salonId, LocalDate from, LocalDate to, Long employeeId);

    /** Clients du salon (agrégés depuis leurs réservations). */
    default List<com.salahtech.BarberShop_Apis.Dtos.SalonClientDto> listClients(Long salonId) {
        return listClients(salonId, null);
    }

    /** Avec {@code employeeId} : uniquement les clients servis par ce prestataire. */
    List<com.salahtech.BarberShop_Apis.Dtos.SalonClientDto> listClients(Long salonId, Long employeeId);
}
