package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.CheckInRequest;
import com.salahtech.BarberShop_Apis.Dtos.QueueEntryDto;
import com.salahtech.BarberShop_Apis.Dtos.QueuePositionDto;
import com.salahtech.BarberShop_Apis.Enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface QueueService {

    QueueEntryDto checkIn(CheckInRequest request);

    /** Crée une entrée de file à partir d'un RDV confirmé (check-in à l'arrivée). */
    QueueEntryDto checkInFromBooking(Long bookingId);

    /**
     * Ajoute un RDV à la file dans une transaction NEUVE (REQUIRES_NEW), sans jamais
     * propager d'erreur. À appeler depuis un afterCommit (le RDV est déjà committé).
     */
    void enqueueFromBookingQuietly(Long bookingId);

    /** Ajoute à la file le client d'une réservation salon confirmée (flux salon-centric). */
    QueueEntryDto enqueueFromSalonBooking(Long salonBookingId);

    /**
     * Variante tolérante, en transaction NEUVE : à appeler APRÈS le commit de la
     * réservation (afterCommit). Un échec de la file ne peut ni annuler ni faire
     * échouer la confirmation / le check-in de la réservation.
     */
    void enqueueFromSalonBookingQuietly(Long salonBookingId);

    /** « Ma place dans la file » : entrée active du client dans un salon (null si aucune). */
    QueuePositionDto myPosition(Long salonId, Long clientId);

    QueueEntryDto callNext(Long salonId, Long workstationId);

    QueueEntryDto callSpecific(Long queueEntryId, Long workstationId);

    QueueEntryDto startService(Long queueEntryId);

    QueueEntryDto complete(Long queueEntryId);

    /** Encaisse le paiement d'une prestation (montant calculé si non fourni). Propagé au RDV si présent. */
    QueueEntryDto pay(Long queueEntryId, PaymentMethod method, BigDecimal amount);

    QueueEntryDto cancel(Long queueEntryId, String reason);

    QueueEntryDto markNoShow(Long queueEntryId);

    QueuePositionDto getPosition(Long queueEntryId);

    List<QueueEntryDto> getActiveQueue(Long salonId);

    List<QueueEntryDto> getWaitingQueue(Long salonId);

    /** Prestations terminées d'une journée (caisse / rattrapage / reçus). */
    List<QueueEntryDto> getCompletedForDay(Long salonId, LocalDate date);

    Map<String, Long> getStats(Long salonId);
}