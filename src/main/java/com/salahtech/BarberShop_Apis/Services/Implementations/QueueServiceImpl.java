package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.CheckInRequest;
import com.salahtech.BarberShop_Apis.Dtos.QueueEntryDto;
import com.salahtech.BarberShop_Apis.Dtos.QueuePositionDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Enums.PaymentMethod;
import com.salahtech.BarberShop_Apis.Enums.PaymentStatus;
import com.salahtech.BarberShop_Apis.Enums.QueueStatus;
import java.math.BigDecimal;
import com.salahtech.BarberShop_Apis.Enums.StationStatus;
import com.salahtech.BarberShop_Apis.Exceptions.BusinessException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.QueueService;
import com.salahtech.BarberShop_Apis.models.*;
import com.salahtech.BarberShop_Apis.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QueueServiceImpl implements QueueService {

    private static final int DEFAULT_SERVICE_DURATION_MIN = 30;
    private static final int NEAR_TURN_POSITION = 2;

    private final QueueEntryRepository queueRepository;
    private final SalonRepository salonRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkstationRepository workstationRepository;
    private final SalonBookingRepository salonBookingRepository;
    private final QueueNotifier notifier;
    private final LoyaltyService loyaltyService;
    private final com.salahtech.BarberShop_Apis.repositories.LoyaltyRewardRepository loyaltyRewardRepository;

    @Override
    public QueueEntryDto checkIn(CheckInRequest req) {
        Salon salon = salonRepository.findById(req.getSalonId())
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable: " + req.getSalonId()));

        QueueEntry entry = QueueEntry.builder()
                .customerName(req.getCustomerName())
                .customerPhone(req.getCustomerPhone())
                .numberOfPeople(req.getNumberOfPeople())
                .salon(salon)
                .checkInTime(LocalDateTime.now())
                .status(QueueStatus.WAITING)
                .notes(req.getNotes())
                .build();

        if (req.getClientId() != null) {
            Client c = clientRepository.findById(req.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client introuvable: " + req.getClientId()));
            entry.setClient(c);
        }

        if (req.getPreferredEmployeeId() != null) {
            Employee e = employeeRepository.findById(req.getPreferredEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Employé introuvable: " + req.getPreferredEmployeeId()));
            entry.setPreferredEmployee(e);
        }

        entry.setTicketNumber(generateTicketNumber(salon.getId()));
        entry.setEstimatedWaitTime(estimateWaitTime(salon.getId(), entry));

        QueueEntry saved = queueRepository.save(entry);
        log.info("Check-in queue id={} ticket={} salon={}", saved.getId(), saved.getTicketNumber(), salon.getId());

        broadcastQueue(salon.getId());
        return toDto(saved);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueFromSalonBookingQuietly(Long salonBookingId) {
        try {
            enqueueFromSalonBooking(salonBookingId);
        } catch (Exception e) {
            log.warn("Auto-ajout file échoué pour réservation salon {} : {}", salonBookingId, e.getMessage());
        }
    }

    /**
     * Ajoute à la file le client d'une réservation salon confirmée (flux salon-centric).
     * Appelé depuis la confirmation, dans la même transaction (l'entité est managée).
     * Idempotent : si une entrée active existe déjà pour ce client+salon aujourd'hui, on la renvoie.
     */
    @Override
    public QueueEntryDto enqueueFromSalonBooking(Long salonBookingId) {
        SalonBooking b = salonBookingRepository.findById(salonBookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable: " + salonBookingId));
        Salon salon = b.getSalon();
        if (salon == null) {
            throw new BusinessException("La réservation n'est rattachée à aucun salon");
        }

        // Idempotence : une entrée active existe déjà pour cette réservation → on la renvoie
        // (évite les doublons quand confirm PUIS « Arrivé » ré-enfilent le même client).
        var existing = queueRepository.findFirstBySalonBookingIdAndStatusInOrderByCheckInTimeDesc(
                b.getId(), List.of(QueueStatus.WAITING, QueueStatus.NOTIFIED, QueueStatus.IN_CHAIR));
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        Client client = b.getClient();
        String name = client != null
                ? ((client.getFirstName() != null ? client.getFirstName() : "") + " "
                        + (client.getLastName() != null ? client.getLastName() : "")).trim()
                : "Client";
        if (name.isEmpty()) name = "Client";

        QueueEntry entry = QueueEntry.builder()
                .customerName(name)
                .customerPhone(client != null ? client.getPhone() : null)
                .numberOfPeople(1)
                .salon(salon)
                .client(client)
                .workstation(b.getAssignedWorkstation())
                .assignedEmployee(b.getAssignedEmployee())
                .checkInTime(LocalDateTime.now())
                .status(QueueStatus.WAITING)
                .salonBookingId(b.getId())
                .expectedPrice(salonBookingPrice(b))
                .notes("Réservation salon #" + b.getId()
                        + (b.getOffering() != null ? " - " + b.getOffering().getName() : ""))
                .build();
        entry.setTicketNumber(generateTicketNumber(salon.getId()));
        entry.setEstimatedWaitTime(estimateWaitTime(salon.getId(), entry));

        QueueEntry saved = queueRepository.save(entry);
        log.info("Réservation salon {} -> file ticket={} salon={}", salonBookingId, saved.getTicketNumber(), salon.getId());
        broadcastQueue(salon.getId());
        return toDto(saved);
    }

    @Override
    public QueueEntryDto callNext(Long salonId, Long workstationId) {
        // Verrou salon : deux postes qui appellent en même temps n'appellent plus le même client.
        salonRepository.lockById(salonId);
        QueueEntry next = queueRepository.findWaitingBySalon(salonId).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("Aucun client en attente"));
        return doCall(next, workstationId);
    }

    @Override
    public QueueEntryDto callSpecific(Long queueEntryId, Long workstationId) {
        QueueEntry entry = queueRepository.findById(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrée file introuvable: " + queueEntryId));
        if (entry.getStatus() != QueueStatus.WAITING) {
            throw new BusinessException("Cette entrée n'est pas en attente (status=" + entry.getStatus() + ")");
        }
        return doCall(entry, workstationId);
    }

    private QueueEntryDto doCall(QueueEntry entry, Long workstationId) {
        Workstation ws = workstationRepository.findById(workstationId)
                .orElseThrow(() -> new ResourceNotFoundException("Workstation introuvable: " + workstationId));
        if (ws.getSalon() == null || entry.getSalon() == null
                || !ws.getSalon().getId().equals(entry.getSalon().getId())) {
            throw new BusinessException("Ce poste n'appartient pas au salon de cette file");
        }
        if (ws.getStatus() == StationStatus.OUT_OF_SERVICE) {
            throw new BusinessException("Ce poste est hors service");
        }
        entry.setWorkstation(ws);
        entry.setAssignedEmployee(ws.getCurrentEmployee());
        entry.setStatus(QueueStatus.NOTIFIED);
        entry.setCalledAt(LocalDateTime.now());

        QueueEntry saved = queueRepository.save(entry);
        notifier.notifyTurnReady(saved);
        broadcastQueue(entry.getSalon().getId());
        notifier.broadcastEntryUpdate(entry.getSalon().getId(), toDto(saved));
        return toDto(saved);
    }

    @Override
    public QueueEntryDto startService(Long queueEntryId) {
        QueueEntry entry = queueRepository.findById(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrée file introuvable: " + queueEntryId));
        if (entry.getStatus() != QueueStatus.NOTIFIED && entry.getStatus() != QueueStatus.WAITING) {
            throw new BusinessException("Impossible de démarrer (status=" + entry.getStatus() + ")");
        }
        entry.setStatus(QueueStatus.IN_CHAIR);
        entry.setStartedAt(LocalDateTime.now());

        if (entry.getWorkstation() != null) {
            Workstation ws = entry.getWorkstation();
            ws.setStatus(StationStatus.OCCUPIED);
            workstationRepository.save(ws);
        }

        QueueEntry saved = queueRepository.save(entry);
        broadcastQueue(entry.getSalon().getId());
        return toDto(saved);
    }

    @Override
    public QueueEntryDto complete(Long queueEntryId) {
        QueueEntry entry = queueRepository.findById(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrée file introuvable: " + queueEntryId));
        if (entry.getStatus() == QueueStatus.COMPLETED) {
            return toDto(entry); // idempotent (double clic / rejeu)
        }
        if (entry.getStatus() == QueueStatus.CANCELLED || entry.getStatus() == QueueStatus.NO_SHOW) {
            throw new BusinessException("Impossible de terminer une entrée " + entry.getStatus());
        }
        entry.setStatus(QueueStatus.COMPLETED);
        entry.setEndedAt(LocalDateTime.now());
        completeLinkedSalonBooking(entry);

        if (entry.getWorkstation() != null) {
            Workstation ws = entry.getWorkstation();
            ws.setStatus(StationStatus.CLEANING);
            workstationRepository.save(ws);
        }

        QueueEntry saved = queueRepository.save(entry);
        broadcastQueue(entry.getSalon().getId());
        return toDto(saved);
    }

    @Override
    public QueueEntryDto pay(Long queueEntryId, PaymentMethod method, BigDecimal amount) {
        return pay(queueEntryId, method, amount, false);
    }

    @Override
    @Transactional
    public QueueEntryDto pay(Long queueEntryId, PaymentMethod method, BigDecimal amount, boolean useReward) {
        QueueEntry entry = queueRepository.findById(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrée file introuvable: " + queueEntryId));

        // Garde : on n'encaisse qu'une prestation en cours ou terminée, et une seule fois.
        if (entry.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Cette prestation est déjà encaissée");
        }
        if (entry.getStatus() != QueueStatus.IN_CHAIR && entry.getStatus() != QueueStatus.COMPLETED) {
            throw new BusinessException("Encaissement impossible (status=" + entry.getStatus()
                    + ") : la prestation doit être en cours ou terminée");
        }

        if (useReward) {
            // Visite offerte (fidélité) : consomme la récompense, encaissement à 0.
            loyaltyService.redeem(entry);
            entry.setAmount(BigDecimal.ZERO);
            entry.setPaymentMethod(method != null ? method : PaymentMethod.CASH);
            entry.setPaymentStatus(PaymentStatus.PAID);
            QueueEntry saved = queueRepository.save(entry);
            broadcastQueue(entry.getSalon().getId());
            return toDto(saved);
        }

        // Montant : celui fourni (permet remise / pourboire), sinon prix calculé.
        BigDecimal amt = (amount != null) ? amount : computePrice(entry);
        // Interdit d'encaisser 0 (aucun prix défini) — mauvais pour le business.
        if (amt == null || amt.signum() <= 0) {
            throw new BusinessException(
                    "Aucun prix défini pour cette prestation : impossible d'encaisser 0. "
                    + "Terminez sans encaisser ou rattachez un service tarifé.");
        }
        entry.setAmount(amt);
        entry.setPaymentMethod(method != null ? method : PaymentMethod.CASH);
        entry.setPaymentStatus(PaymentStatus.PAID);

        // Propagation au RDV si l'entrée provient d'une réservation.

        QueueEntry saved = queueRepository.save(entry);
        broadcastQueue(entry.getSalon().getId());
        return toDto(saved);
    }

    /**
     * Fin de prestation → la réservation salon d'origine passe à COMPLETED
     * (débloque l'avis client et les stats « terminés » / CA).
     */
    private void completeLinkedSalonBooking(QueueEntry entry) {
        if (entry.getSalonBookingId() == null) return;
        salonBookingRepository.findById(entry.getSalonBookingId()).ifPresent(sb -> {
            if (sb.getStatus() != BookingStatus.CANCELLED && sb.getStatus() != BookingStatus.COMPLETED) {
                sb.setStatus(BookingStatus.COMPLETED);
                salonBookingRepository.save(sb);
            }
        });
    }

    /** Libère le poste d'une prestation interrompue. */
    private void releaseWorkstation(QueueEntry entry) {
        Workstation ws = entry.getWorkstation();
        if (ws != null && ws.getStatus() == StationStatus.OCCUPIED) {
            ws.setStatus(StationStatus.FREE);
            workstationRepository.save(ws);
        }
    }

    /** Montant à encaisser : prix attendu (service réservé) > total du RDV > somme des services. */
    private BigDecimal computePrice(QueueEntry e) {
        if (e.getExpectedPrice() != null && e.getExpectedPrice().signum() > 0) {
            return e.getExpectedPrice();
        }
        return BigDecimal.ZERO;
    }

    /** Prix d'une réservation salon : total explicite, sinon prix de l'offre. */
    private BigDecimal salonBookingPrice(SalonBooking b) {
        if (b.getTotalPrice() != null && b.getTotalPrice().signum() > 0) {
            return b.getTotalPrice();
        }
        if (b.getOffering() != null && b.getOffering().getPrice() != null) {
            return b.getOffering().getPrice();
        }
        return null;
    }

    @Override
    public QueueEntryDto cancel(Long queueEntryId, String reason) {
        QueueEntry entry = queueRepository.findById(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrée file introuvable: " + queueEntryId));
        if (entry.getStatus() == QueueStatus.CANCELLED) {
            return toDto(entry); // idempotent
        }
        if (entry.getStatus() == QueueStatus.COMPLETED || entry.getStatus() == QueueStatus.NO_SHOW) {
            throw new BusinessException("Impossible d'annuler une entrée " + entry.getStatus());
        }
        if (entry.getStatus() == QueueStatus.IN_CHAIR) {
            releaseWorkstation(entry);
        }
        entry.setStatus(QueueStatus.CANCELLED);
        entry.setEndedAt(LocalDateTime.now());
        if (reason != null && !reason.isBlank()) {
            entry.setNotes((entry.getNotes() == null ? "" : entry.getNotes() + " | ") + "Annulé: " + reason);
        }
        QueueEntry saved = queueRepository.save(entry);
        broadcastQueue(entry.getSalon().getId());
        return toDto(saved);
    }

    @Override
    public QueueEntryDto markNoShow(Long queueEntryId) {
        QueueEntry entry = queueRepository.findById(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrée file introuvable: " + queueEntryId));
        if (entry.getStatus() != QueueStatus.WAITING && entry.getStatus() != QueueStatus.NOTIFIED) {
            throw new BusinessException("Absent impossible (status=" + entry.getStatus()
                    + ") : seulement pour un client en attente ou appelé");
        }
        entry.setStatus(QueueStatus.NO_SHOW);
        entry.setEndedAt(LocalDateTime.now());
        QueueEntry saved = queueRepository.save(entry);
        broadcastQueue(entry.getSalon().getId());
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public QueuePositionDto getPosition(Long queueEntryId) {
        QueueEntry entry = queueRepository.findById(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrée file introuvable: " + queueEntryId));

        long ahead = entry.getStatus() == QueueStatus.WAITING
                ? queueRepository.countAheadOf(entry.getSalon().getId(), entry.getCheckInTime())
                : 0;
        int position = (int) ahead + 1;
        int wait = estimateWaitTime(entry.getSalon().getId(), entry);

        return QueuePositionDto.builder()
                .queueEntryId(entry.getId())
                .ticketNumber(entry.getTicketNumber())
                .status(entry.getStatus())
                .position(position)
                .peopleAhead((int) ahead)
                .estimatedWaitMinutes(wait)
                .workstationName(entry.getWorkstation() != null ? entry.getWorkstation().getName() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueEntryDto> myActiveEntries(Long clientId) {
        if (clientId == null) return List.of();
        return queueRepository.findActiveByClient(clientId,
                        List.of(QueueStatus.WAITING, QueueStatus.NOTIFIED, QueueStatus.IN_CHAIR))
                .stream().map(e -> {
                    QueueEntryDto d = toDto(e);
                    if (e.getStatus() == QueueStatus.WAITING) {
                        d.setEstimatedWaitTime(estimateWaitTime(e.getSalon().getId(), e));
                    }
                    return d;
                })
                .collect(Collectors.toList());
    }

    /** « Ma place dans la file » : entrée active du client dans un salon (ou null). */
    @Override
    @Transactional(readOnly = true)
    public QueuePositionDto myPosition(Long salonId, Long clientId) {
        if (clientId == null) return null;
        List<QueueEntry> active = queueRepository.findActiveByClientAndSalon(
                salonId, clientId,
                List.of(QueueStatus.WAITING, QueueStatus.NOTIFIED, QueueStatus.IN_CHAIR));
        if (active.isEmpty()) return null;
        return getPosition(active.get(0).getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueEntryDto> getActiveQueue(Long salonId) {
        List<QueueStatus> active = List.of(QueueStatus.WAITING, QueueStatus.NOTIFIED, QueueStatus.IN_CHAIR);
        return queueRepository.findActiveBySalon(salonId, active).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueEntryDto> getWaitingQueue(Long salonId) {
        return queueRepository.findWaitingBySalon(salonId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueEntryDto> getCompletedForDay(Long salonId, java.time.LocalDate date) {
        java.time.LocalDate d = (date != null) ? date : LocalDate.now();
        LocalDateTime start = d.atStartOfDay();
        LocalDateTime end = d.plusDays(1).atStartOfDay();
        return queueRepository.findCompletedBySalonAndDay(salonId, start, end).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getStats(Long salonId) {
        Map<String, Long> stats = new HashMap<>();
        for (QueueStatus s : QueueStatus.values()) {
            stats.put(s.name(), queueRepository.countBySalon_IdAndStatus(salonId, s));
        }
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        stats.put("TODAY_TOTAL", queueRepository.countByDayBySalon(salonId, startOfDay));
        return stats;
    }

    private String generateTicketNumber(Long salonId) {
        // Verrou salon (jusqu'au commit) : deux check-in simultanés n'obtiennent plus le même numéro.
        salonRepository.lockById(salonId);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long todayCount = queueRepository.countByDayBySalon(salonId, startOfDay);
        return String.format("A-%03d", todayCount + 1);
    }

    private int estimateWaitTime(Long salonId, QueueEntry entry) {
        long peopleAhead = entry.getCheckInTime() != null
                ? queueRepository.countAheadOf(salonId, entry.getCheckInTime())
                : queueRepository.countBySalon_IdAndStatus(salonId, QueueStatus.WAITING);

        int avgServiceMinutes = DEFAULT_SERVICE_DURATION_MIN;

        long activeStations = workstationRepository.countBySalon_IdAndStatus(salonId, StationStatus.OCCUPIED)
                + workstationRepository.countBySalon_IdAndStatus(salonId, StationStatus.FREE);
        int parallel = (int) Math.max(1, activeStations);

        return (int) Math.ceil((double) peopleAhead * avgServiceMinutes / parallel);
    }

    private void broadcastQueue(Long salonId) {
        List<QueueEntryDto> queue = getActiveQueue(salonId);
        notifier.broadcastQueueUpdate(salonId, queue);
        notifyApproachingTurns(salonId, queue);
    }

    private void notifyApproachingTurns(Long salonId, List<QueueEntryDto> queue) {
        int idx = 0;
        for (QueueEntryDto dto : queue) {
            if (dto.getStatus() == QueueStatus.WAITING) {
                idx++;
                if (idx <= NEAR_TURN_POSITION) {
                    queueRepository.findById(dto.getId())
                            .ifPresent(e -> notifier.notifyTurnApproaching(e, queue.indexOf(dto) + 1));
                }
            }
        }
    }

    private QueueEntryDto toDto(QueueEntry e) {
        long ahead = e.getStatus() == QueueStatus.WAITING && e.getCheckInTime() != null
                ? queueRepository.countAheadOf(e.getSalon().getId(), e.getCheckInTime())
                : 0;

        Employee assigned = e.getAssignedEmployee();
        Workstation ws = e.getWorkstation();

        return QueueEntryDto.builder()
                .id(e.getId())
                .ticketNumber(e.getTicketNumber())
                .customerName(e.getCustomerName())
                .customerPhone(e.getCustomerPhone())
                .numberOfPeople(e.getNumberOfPeople())
                .salonId(e.getSalon() != null ? e.getSalon().getId() : null)
                .salonName(e.getSalon() != null ? e.getSalon().getName() : null)
                .clientId(e.getClient() != null ? e.getClient().getId() : null)
                .preferredEmployeeId(e.getPreferredEmployee() != null ? e.getPreferredEmployee().getId() : null)
                .assignedEmployeeId(assigned != null ? assigned.getId() : null)
                .assignedEmployeeName(assigned != null ? (assigned.getFirstName() + " " + assigned.getLastName()) : null)
                .workstationId(ws != null ? ws.getId() : null)
                .workstationName(ws != null ? ws.getName() : null)
                .checkInTime(e.getCheckInTime())
                .calledAt(e.getCalledAt())
                .startedAt(e.getStartedAt())
                .endedAt(e.getEndedAt())
                .estimatedWaitTime(e.getEstimatedWaitTime())
                .status(e.getStatus())
                .statusDisplayName(e.getStatus() != null ? e.getStatus().getDisplayName() : null)
                .position(e.getStatus() == QueueStatus.WAITING ? (int) ahead + 1 : null)
                .notes(e.getNotes())
                .paymentStatus(e.getPaymentStatus())
                .paymentMethod(e.getPaymentMethod())
                .amount(e.getAmount())
                .estimatedPrice(computePrice(e))
                .rewardAvailable(loyaltyService.rewardAvailable(e))
                .rewardUsed(e.getId() != null && loyaltyRewardRepository.existsByQueueEntry_Id(e.getId()))
                .build();
    }
}