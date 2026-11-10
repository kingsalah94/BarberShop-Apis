package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.models.SalonBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SalonBookingRepository extends JpaRepository<SalonBooking, Long> {

    /** Réservations d'un salon sur un intervalle, filtrées par statut (pour le calcul de créneaux). */
    List<SalonBooking> findBySalon_IdAndStatusInAndStartAtBetween(
            Long salonId, Collection<BookingStatus> statuses, LocalDateTime from, LocalDateTime to);

    /** Toutes les réservations d'un salon sur un intervalle (vue staff). */
    List<SalonBooking> findBySalon_IdAndStartAtBetweenOrderByStartAtAsc(
            Long salonId, LocalDateTime from, LocalDateTime to);

    /** Réservations d'un client. */
    List<SalonBooking> findByClient_IdOrderByStartAtDesc(Long clientId);

    /** Toutes les réservations d'un salon (pour agréger les clients). */
    List<SalonBooking> findBySalon_Id(Long salonId);

    // ===== Tableau de bord plateforme (ADMIN) =====

    long countByStatus(BookingStatus status);

    /** Toutes les réservations, tous salons, sur un intervalle et des statuts (rappels). */
    List<SalonBooking> findByStatusInAndStartAtBetween(
            Collection<BookingStatus> statuses, LocalDateTime from, LocalDateTime to);

    List<SalonBooking> findTop6ByStartAtGreaterThanEqualAndStatusInOrderByStartAtAsc(
            LocalDateTime from, Collection<BookingStatus> statuses);

    /** Intervalle semi-ouvert [from, to[. */
    long countByStartAtGreaterThanEqualAndStartAtLessThan(LocalDateTime from, LocalDateTime to);

    long countByStatusAndStartAtGreaterThanEqualAndStartAtLessThan(
            BookingStatus status, LocalDateTime from, LocalDateTime to);

    /** Chiffre d'affaires = prestations terminées. */
    @org.springframework.data.jpa.repository.Query(
            "select coalesce(sum(b.totalPrice), 0) from SalonBooking b where b.status = "
            + "com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED")
    java.math.BigDecimal sumCompletedRevenue();

    @org.springframework.data.jpa.repository.Query(
            "select coalesce(sum(b.totalPrice), 0) from SalonBooking b where b.status = "
            + "com.salahtech.BarberShop_Apis.Enums.BookingStatus.COMPLETED "
            + "and b.startAt >= :from and b.startAt < :to")
    java.math.BigDecimal sumCompletedRevenueBetween(
            @org.springframework.data.repository.query.Param("from") LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") LocalDateTime to);

    // ----- Fidélité -----
    long countByClient_IdAndSalon_IdAndStatus(Long clientId, Long salonId,
                                              com.salahtech.BarberShop_Apis.Enums.BookingStatus status);

    List<SalonBooking> findByClient_IdAndStatus(Long clientId,
                                                com.salahtech.BarberShop_Apis.Enums.BookingStatus status);

    boolean existsBySalon_IdAndClient_Id(Long salonId, Long clientId);
}
