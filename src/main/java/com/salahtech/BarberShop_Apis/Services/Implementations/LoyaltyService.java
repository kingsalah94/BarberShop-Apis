package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.LoyaltyStatusDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Enums.QueueStatus;
import com.salahtech.BarberShop_Apis.Exceptions.BusinessException;
import com.salahtech.BarberShop_Apis.models.LoyaltyReward;
import com.salahtech.BarberShop_Apis.models.QueueEntry;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.repositories.LoyaltyRewardRepository;
import com.salahtech.BarberShop_Apis.repositories.QueueEntryRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fidélité « 10 visites payées = 1 prestation offerte », par salon.
 * <ul>
 *   <li>Visite = réservation salon COMPLETED, ou passage en file COMPLETED d'un client identifié sans RDV
 *       (un RDV passé par la file n'est compté qu'une fois, via la réservation).</li>
 *   <li>Une prestation offerte consommée ne compte pas comme visite payée.</li>
 *   <li>Récompenses disponibles = floor(visites payées / 10) − récompenses consommées.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    public static final int CARD_SIZE = 10;

    private final SalonBookingRepository bookingRepository;
    private final QueueEntryRepository queueRepository;
    private final LoyaltyRewardRepository rewardRepository;

    /** Carte du client dans un salon. */
    @Transactional(readOnly = true)
    public LoyaltyStatusDto status(Long clientId, Long salonId, String salonName) {
        long visits = bookingRepository.countByClient_IdAndSalon_IdAndStatus(clientId, salonId, BookingStatus.COMPLETED)
                + queueRepository.countBySalon_IdAndClient_IdAndStatusAndSalonBookingIdIsNull(salonId, clientId, QueueStatus.COMPLETED);
        long used = rewardRepository.countByClient_IdAndSalon_Id(clientId, salonId);
        return build(salonId, salonName, visits, used);
    }

    static LoyaltyStatusDto build(Long salonId, String salonName, long completedVisits, long used) {
        long paid = Math.max(0, completedVisits - used);
        long earned = paid / CARD_SIZE;
        return LoyaltyStatusDto.builder()
                .salonId(salonId)
                .salonName(salonName)
                .cardSize(CARD_SIZE)
                .paidVisits(paid)
                .stamps(paid % CARD_SIZE)
                .rewardsEarned(earned)
                .rewardsUsed(used)
                .rewardsAvailable(Math.max(0, earned - used))
                .build();
    }

    /** Toutes les cartes du client (salons où il a au moins une visite), les plus remplies d'abord. */
    @Transactional(readOnly = true)
    public List<LoyaltyStatusDto> mine(Long clientId) {
        Map<Long, String> salons = new LinkedHashMap<>();
        Map<Long, Long> visits = new LinkedHashMap<>();
        bookingRepository.findByClient_IdAndStatus(clientId, BookingStatus.COMPLETED).forEach(b -> {
            Salon s = b.getSalon();
            salons.putIfAbsent(s.getId(), s.getName());
            visits.merge(s.getId(), 1L, Long::sum);
        });
        queueRepository.findByClient_IdAndStatusAndSalonBookingIdIsNull(clientId, QueueStatus.COMPLETED).forEach(q -> {
            Salon s = q.getSalon();
            salons.putIfAbsent(s.getId(), s.getName());
            visits.merge(s.getId(), 1L, Long::sum);
        });
        return salons.entrySet().stream()
                .map(e -> build(e.getKey(), e.getValue(), visits.getOrDefault(e.getKey(), 0L),
                        rewardRepository.countByClient_IdAndSalon_Id(clientId, e.getKey())))
                .sorted(Comparator.comparingLong(LoyaltyStatusDto::getPaidVisits).reversed())
                .toList();
    }

    /** Le client de cette entrée a-t-il une prestation offerte à utiliser dans ce salon ? */
    @Transactional(readOnly = true)
    public boolean rewardAvailable(QueueEntry entry) {
        if (entry.getClient() == null || entry.getSalon() == null) return false;
        return status(entry.getClient().getId(), entry.getSalon().getId(), null).getRewardsAvailable() > 0;
    }

    /** Consomme une prestation offerte pour cette entrée (appelé à l'encaissement). */
    @Transactional
    public void redeem(QueueEntry entry) {
        if (entry.getClient() == null) {
            throw new BusinessException("Client non identifié : la visite offerte nécessite un compte client.");
        }
        if (rewardRepository.existsByQueueEntry_Id(entry.getId())) {
            throw new BusinessException("Une visite offerte est déjà appliquée à cette prestation.");
        }
        if (!rewardAvailable(entry)) {
            throw new BusinessException("Aucune visite offerte disponible pour ce client dans ce salon.");
        }
        rewardRepository.save(LoyaltyReward.builder()
                .client(entry.getClient())
                .salon(entry.getSalon())
                .queueEntry(entry)
                .build());
    }
}
