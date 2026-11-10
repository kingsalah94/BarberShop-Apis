package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.CreateSalonBookingRequest;
import com.salahtech.BarberShop_Apis.Dtos.EffectiveHoursDto;
import com.salahtech.BarberShop_Apis.Dtos.TimeWindowDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonBookingDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonClientDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonStatsDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Exceptions.BusinessException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonBookingService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonHoursService;
import com.salahtech.BarberShop_Apis.models.*;
import com.salahtech.BarberShop_Apis.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SalonBookingServiceImpl implements SalonBookingService {

    /** Pas de la grille de créneaux (minutes). */
    private static final int SLOT_STEP_MIN = 15;
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    /** Statuts qui « occupent » un créneau. */
    private static final List<BookingStatus> ACTIVE =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.BOOKED);

    private final SalonBookingRepository bookingRepository;
    private final SalonOfferingRepository offeringRepository;
    private final SalonRepository salonRepository;
    private final WorkstationRepository workstationRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeTimeOffRepository timeOffRepository;
    private final SalonReviewRepository reviewRepository;
    private final SalonHoursService hoursService;
    private final com.salahtech.BarberShop_Apis.Services.Interfaces.QueueService queueService;
    private final com.salahtech.BarberShop_Apis.Services.Interfaces.NotificationService notificationService;
    private final com.salahtech.BarberShop_Apis.Services.Interfaces.DeviceTokenService deviceTokenService;

    private Salon salon(Long id) {
        return salonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable: " + id));
    }

    private SalonOffering offering(Long salonId, Long serviceId) {
        SalonOffering o = offeringRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service introuvable: " + serviceId));
        if (o.getSalon() == null || !o.getSalon().getId().equals(salonId)) {
            throw new BusinessException("Ce service n'appartient pas au salon " + salonId);
        }
        return o;
    }

    /** Capacité parallèle = nombre de postes actifs (min 1). */
    private int capacity(Long salonId) {
        int n = workstationRepository.findBySalon_IdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(salonId).size();
        return Math.max(n, 1);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> availableSlots(Long salonId, Long serviceId, LocalDate date, Long employeeId) {
        salon(salonId);
        SalonOffering svc = offering(salonId, serviceId);
        int duration = svc.getDurationMinutes() != null ? svc.getDurationMinutes() : 30;

        EffectiveHoursDto eff = hoursService.getEffectiveHours(salonId, date);
        List<TimeWindowDto> windows = effWindows(eff);
        if (windows.isEmpty()) {
            return List.of();
        }

        // Prestataire choisi : absent ce jour-là (congé / inactif) → aucun créneau.
        Employee chosen = null;
        if (employeeId != null) {
            chosen = employeeOfSalon(salonId, employeeId);
            if (!isBookable(chosen, date)) {
                return List.of();
            }
        }

        int cap = capacity(salonId);
        LocalDateTime dayStart = date.atStartOfDay();
        List<SalonBooking> dayBookings = bookingRepository
                .findBySalon_IdAndStatusInAndStartAtBetween(salonId, ACTIVE, dayStart, dayStart.plusDays(1));
        List<SalonBooking> employeeBookings = chosen == null ? List.of() : assignedTo(dayBookings, chosen.getId());

        LocalDateTime now = LocalDateTime.now();
        boolean today = date.equals(LocalDate.now());

        // Un jeu de créneaux par plage d'ouverture (la pause déjeuner est ainsi exclue).
        List<String> slots = new java.util.ArrayList<>();
        for (TimeWindowDto w : windows) {
            slots.addAll(buildSlots(date, w.getOpenAt(), w.getCloseAt(), duration, cap, dayBookings,
                    employeeBookings, today, now));
        }
        return slots;
    }

    /** Plages effectives d'ouverture (rétro-compat : openAt/closeAt = plage unique si {@code windows} absent). */
    private List<TimeWindowDto> effWindows(EffectiveHoursDto eff) {
        if (eff == null || eff.isClosed()) return List.of();
        if (eff.getWindows() != null && !eff.getWindows().isEmpty()) return eff.getWindows();
        if (eff.getOpenAt() != null && eff.getCloseAt() != null) {
            return List.of(TimeWindowDto.builder().openAt(eff.getOpenAt()).closeAt(eff.getCloseAt()).build());
        }
        return List.of();
    }

    private List<String> buildSlots(LocalDate date, LocalTime open, LocalTime close, int duration,
                                    int capacity, List<SalonBooking> dayBookings,
                                    List<SalonBooking> employeeBookings, boolean today, LocalDateTime now) {
        List<String> slots = new java.util.ArrayList<>();
        for (LocalTime t = open; !t.plusMinutes(duration).isAfter(close); t = t.plusMinutes(SLOT_STEP_MIN)) {
            LocalDateTime start = date.atTime(t);
            LocalDateTime end = start.plusMinutes(duration);
            if (today && start.isBefore(now)) continue; // pas de créneau passé
            long overlap = dayBookings.stream()
                    .filter(b -> b.getStartAt().isBefore(end) && b.getEndAt().isAfter(start))
                    .count();
            boolean employeeFree = employeeBookings.stream().noneMatch(b -> overlaps(b, start, end));
            if (overlap < capacity && employeeFree) slots.add(t.format(HHMM));
        }
        return slots;
    }

    @Override
    public SalonBookingDto create(Long salonId, CreateSalonBookingRequest req, ApplicationUser user) {
        Salon salon = salon(salonId);
        SalonOffering svc = offering(salonId, req.getServiceId());
        Client client = user != null ? user.getClient() : null;
        if (client == null) {
            throw new BusinessException("Un profil client est requis pour réserver");
        }

        int duration = svc.getDurationMinutes() != null ? svc.getDurationMinutes() : 30;
        LocalTime t;
        try {
            t = LocalTime.parse(req.getTime(), HHMM);
        } catch (Exception e) {
            throw new BusinessException("Heure invalide (format HH:mm attendu): " + req.getTime());
        }
        LocalDateTime start = req.getDate().atTime(t);
        LocalDateTime end = start.plusMinutes(duration);

        if (start.isBefore(LocalDateTime.now())) {
            throw new BusinessException("Impossible de réserver dans le passé");
        }

        EffectiveHoursDto eff = hoursService.getEffectiveHours(salonId, req.getDate());
        List<TimeWindowDto> windows = effWindows(eff);
        if (windows.isEmpty()) {
            throw new BusinessException("Le salon est fermé à cette date");
        }
        LocalTime endTime = end.toLocalTime();
        boolean fits = windows.stream()
                .anyMatch(w -> !t.isBefore(w.getOpenAt()) && !endTime.isAfter(w.getCloseAt()));
        if (!fits) {
            throw new BusinessException("Créneau hors des horaires d'ouverture");
        }

        // Re-vérifie la capacité au moment de la réservation (anti double-booking).
        // Verrou sur le salon : deux réservations simultanées ne peuvent plus lire
        // la même capacité restante puis insérer toutes les deux.
        salonRepository.lockById(salonId);
        int cap = capacity(salonId);
        long overlap = bookingRepository
                .findBySalon_IdAndStatusInAndStartAtBetween(salonId, ACTIVE,
                        req.getDate().atStartOfDay(), req.getDate().atStartOfDay().plusDays(1))
                .stream()
                .filter(b -> b.getStartAt().isBefore(end) && b.getEndAt().isAfter(start))
                .count();
        if (overlap >= cap) {
            throw new BusinessException("Ce créneau n'est plus disponible");
        }

        // Prestataire choisi par le client : rattaché au salon, présent ce jour-là, libre sur le créneau.
        Employee chosen = null;
        if (req.getEmployeeId() != null) {
            chosen = employeeOfSalon(salonId, req.getEmployeeId());
            if (!isBookable(chosen, req.getDate())) {
                throw new BusinessException("Ce prestataire n'est pas disponible à cette date");
            }
            boolean busy = assignedTo(bookingRepository
                    .findBySalon_IdAndStatusInAndStartAtBetween(salonId, ACTIVE,
                            req.getDate().atStartOfDay(), req.getDate().atStartOfDay().plusDays(1)), chosen.getId())
                    .stream()
                    .anyMatch(b -> overlaps(b, start, end));
            if (busy) {
                throw new BusinessException("Ce prestataire n'est plus disponible sur ce créneau");
            }
        }

        SalonBooking b = SalonBooking.builder()
                .salon(salon)
                .offering(svc)
                .client(client)
                .startAt(start)
                .endAt(end)
                .status(BookingStatus.PENDING)
                .totalPrice(svc.getPrice())
                .notes(req.getNotes())
                .assignedEmployee(chosen)
                .build();
        SalonBooking saved = bookingRepository.save(b);
        log.info("SalonBooking créée id={} salon={} service={} client={} {}",
                saved.getId(), salonId, svc.getId(), client.getId(), start);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalonBookingDto> listForSalon(Long salonId, LocalDate date) {
        return listForSalon(salonId, date, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalonBookingDto> listForSalon(Long salonId, LocalDate date, Long employeeId) {
        LocalDate d = date != null ? date : LocalDate.now();
        return listForSalonRange(salonId, d, d, employeeId);
    }

    /** Période maximale acceptée par {@link #listForSalonRange} (≈ un trimestre). */
    static final int MAX_RANGE_DAYS = 92;

    @Override
    @Transactional(readOnly = true)
    public List<SalonBookingDto> listForSalonRange(Long salonId, LocalDate from, LocalDate to, Long employeeId) {
        salon(salonId);
        LocalDate f = from != null ? from : LocalDate.now();
        LocalDate t = to != null ? to : f;
        if (t.isBefore(f)) {
            throw new com.salahtech.BarberShop_Apis.Exceptions.BadRequestException("La date de fin précède la date de début");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(f, t) >= MAX_RANGE_DAYS) {
            throw new com.salahtech.BarberShop_Apis.Exceptions.BadRequestException("Période trop longue (" + MAX_RANGE_DAYS + " jours max)");
        }
        return bookingRepository
                .findBySalon_IdAndStartAtBetweenOrderByStartAtAsc(salonId, f.atStartOfDay(), t.plusDays(1).atStartOfDay())
                .stream()
                .filter(b -> employeeId == null
                        || (b.getAssignedEmployee() != null && employeeId.equals(b.getAssignedEmployee().getId())))
                .map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public SalonBookingDto assignEmployee(Long salonId, Long bookingId, Long employeeId) {
        SalonBooking b = owned(salonId, bookingId);
        b.setAssignedEmployee(employeeId == null ? null
                : resolveAssignable(salonId, employeeId, b.getStartAt().toLocalDate()));
        return toDto(bookingRepository.save(b));
    }

    /** Charge un employé, vérifie son rattachement au salon ET qu'il n'est pas en congé ce jour-là. */
    private Employee resolveAssignable(Long salonId, Long employeeId, LocalDate date) {
        Employee e = employeeOfSalon(salonId, employeeId);
        if (timeOffRepository.existsByEmployee_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(e.getId(), date, date)) {
            throw new BusinessException("Ce prestataire est en congé à cette date");
        }
        return e;
    }

    private static boolean overlaps(SalonBooking b, LocalDateTime start, LocalDateTime end) {
        return b.getStartAt().isBefore(end) && b.getEndAt().isAfter(start);
    }

    private static List<SalonBooking> assignedTo(List<SalonBooking> bookings, Long employeeId) {
        return bookings.stream()
                .filter(b -> b.getAssignedEmployee() != null && employeeId.equals(b.getAssignedEmployee().getId()))
                .collect(Collectors.toList());
    }

    /** Réservable ce jour-là : actif et pas en congé. */
    private boolean isBookable(Employee e, LocalDate date) {
        return e.isActive() && !timeOffRepository
                .existsByEmployee_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(e.getId(), date, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.salahtech.BarberShop_Apis.Dtos.SalonStaffDto> bookableStaff(Long salonId) {
        salon(salonId);
        return employeeRepository.findBySalonIdAndRole(salonId, com.salahtech.BarberShop_Apis.Enums.EmployeeRole.BARBER)
                .stream()
                .map(e -> {
                    String last = e.getLastName() == null || e.getLastName().isBlank() ? ""
                            : " " + e.getLastName().substring(0, 1).toUpperCase() + ".";
                    String photo = e.getBarber() != null && e.getBarber().getProfileImageUrl() != null
                            ? e.getBarber().getProfileImageUrl()
                            : (e.getUser() != null ? e.getUser().getProfileImageUrl() : null);
                    return com.salahtech.BarberShop_Apis.Dtos.SalonStaffDto.builder()
                            .id(e.getId())
                            .displayName((e.getFirstName() == null ? "" : e.getFirstName()) + last)
                            .photoUrl(photo)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalonBookingDto> upcomingPlatform() {
        return bookingRepository
                .findTop6ByStartAtGreaterThanEqualAndStatusInOrderByStartAtAsc(LocalDateTime.now(), ACTIVE)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    /** Charge un employé et vérifie qu'il est rattaché au salon. */
    private Employee employeeOfSalon(Long salonId, Long employeeId) {
        Employee e = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Prestataire introuvable: " + employeeId));
        boolean inSalon = e.getSalons().stream().anyMatch(s -> s.getId().equals(salonId))
                || (e.getPrimarySalon() != null && e.getPrimarySalon().getId().equals(salonId));
        if (!inSalon) {
            throw new BusinessException("Ce prestataire n'appartient pas au salon " + salonId);
        }
        return e;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalonBookingDto> listMine(ApplicationUser user) {
        Client client = user != null ? user.getClient() : null;
        if (client == null) return List.of();
        return bookingRepository.findByClient_IdOrderByStartAtDesc(client.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public SalonBookingDto confirm(Long salonId, Long bookingId, Long workstationId, Long employeeId) {
        SalonBooking b = owned(salonId, bookingId);
        if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Réservation " + b.getStatus() + " : confirmation impossible");
        }
        boolean wasPending = b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.BOOKED;
        b.setStatus(BookingStatus.CONFIRMED);
        assignStation(b, salonId, workstationId);
        // Affectation directe d'un prestataire (option A) — prioritaire sur le titulaire du poste.
        if (employeeId != null) b.setAssignedEmployee(resolveAssignable(salonId, employeeId, b.getStartAt().toLocalDate()));
        SalonBooking saved = bookingRepository.save(b);

        // Réservation du JOUR confirmée → ajout automatique à la file d'attente du salon.
        if (wasPending && saved.getStartAt() != null
                && saved.getStartAt().toLocalDate().equals(LocalDate.now())) {
            enqueueAfterCommit(saved.getId());
        }
        // Notifie le client que son rendez-vous est confirmé (uniquement lors d'une vraie confirmation).
        if (wasPending) notifyClientConfirmed(saved);
        return toDto(saved);
    }

    /** Push au client : « Rendez-vous confirmé » (no-op silencieux si pas de device token). */
    private void notifyClientConfirmed(SalonBooking b) {
        String svc = b.getOffering() != null ? b.getOffering().getName() : "votre prestation";
        String when = b.getStartAt() != null ? b.getStartAt().format(DateTimeFormatter.ofPattern("dd/MM 'à' HH:mm")) : "";
        pushToClient(b, "BOOKING_CONFIRMED", "Rendez-vous confirmé",
                "Votre RDV (" + svc + ") " + when + " est confirmé.");
    }

    @Override
    @Transactional(readOnly = true)
    public int sendRemindersFor(LocalDate day) {
        int sent = 0;
        for (SalonBooking b : bookingRepository.findByStatusInAndStartAtBetween(
                List.of(BookingStatus.CONFIRMED, BookingStatus.BOOKED),
                day.atStartOfDay(), day.plusDays(1).atStartOfDay())) {
            String svc = b.getOffering() != null ? b.getOffering().getName() : "votre prestation";
            String salonName = b.getSalon() != null ? b.getSalon().getName() : "le salon";
            String at = b.getStartAt().format(DateTimeFormatter.ofPattern("HH:mm"));
            if (pushToClient(b, "BOOKING_REMINDER", "Rappel : rendez-vous demain",
                    svc + " à " + at + " chez " + salonName + ".")) {
                sent++;
            }
        }
        log.info("Rappels RDV salon du {} : {} envoyé(s)", day, sent);
        return sent;
    }

    /** Push au client de la réservation (sans appareil enregistré : rien). Jamais d'exception. */
    private boolean pushToClient(SalonBooking b, String type, String title, String body) {
        try {
            Client c = b.getClient();
            if (c == null || c.getUser() == null) return false;
            Long userId = c.getUser().getId();
            if (!deviceTokenService.hasActiveToken(userId)) return false;
            String token = deviceTokenService.getDeviceToken(userId);
            if (token == null || token.isBlank()) return false;

            Map<String, String> data = new java.util.HashMap<>();
            data.put("type", type);
            data.put("salonBookingId", String.valueOf(b.getId()));
            notificationService.sendNotification(token, title, body, data);
            return true;
        } catch (Exception e) {
            log.warn("Notif {} RDV salon {} échouée : {}", type, b.getId(), e.getMessage());
            return false;
        }
    }

    @Override
    public SalonBookingDto checkIn(Long salonId, Long bookingId, Long workstationId, Long employeeId) {
        SalonBooking b = owned(salonId, bookingId);
        if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Réservation " + b.getStatus() + " : check-in impossible");
        }
        // Confirme si encore en attente + affecte éventuellement poste / prestataire.
        if (b.getStatus() == BookingStatus.PENDING) b.setStatus(BookingStatus.CONFIRMED);
        assignStation(b, salonId, workstationId);
        if (employeeId != null) b.setAssignedEmployee(resolveAssignable(salonId, employeeId, b.getStartAt().toLocalDate()));
        SalonBooking saved = bookingRepository.save(b);
        enqueueAfterCommit(saved.getId());
        return toDto(saved);
    }

    /**
     * Ajout en file APRÈS le commit, en transaction séparée : une exception de la file
     * ne marque plus la transaction de la réservation « rollback-only » (avant : le
     * catch masquait l'erreur mais confirm/check-in échouait au commit en 500).
     */
    private void enqueueAfterCommit(Long salonBookingId) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            queueService.enqueueFromSalonBookingQuietly(salonBookingId);
                        }
                    });
        } else {
            queueService.enqueueFromSalonBookingQuietly(salonBookingId);
        }
    }

    /** Affecte un poste (et son titulaire éventuel) à la réservation, en vérifiant le salon. */
    private void assignStation(SalonBooking b, Long salonId, Long workstationId) {
        if (workstationId == null) return;
        Workstation ws = workstationRepository.findById(workstationId)
                .orElseThrow(() -> new ResourceNotFoundException("Poste introuvable: " + workstationId));
        if (ws.getSalon() == null || !ws.getSalon().getId().equals(salonId)) {
            throw new BusinessException("Ce poste n'appartient pas au salon " + salonId);
        }
        b.setAssignedWorkstation(ws);
        // Titulaire du poste affecté automatiquement — sauf s'il est en congé ce jour-là.
        if (ws.getCurrentEmployee() != null && b.getAssignedEmployee() == null) {
            final Long empId = ws.getCurrentEmployee().getId();
            final LocalDate d = b.getStartAt().toLocalDate();
            boolean onLeave = timeOffRepository
                    .existsByEmployee_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(empId, d, d);
            if (!onLeave) b.setAssignedEmployee(ws.getCurrentEmployee());
        }
    }

    @Override
    public SalonBookingDto cancel(Long salonId, Long bookingId, ApplicationUser user) {
        SalonBooking b = owned(salonId, bookingId);
        if (b.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Une réservation terminée ne peut pas être annulée");
        }
        b.setStatus(BookingStatus.CANCELLED);
        return toDto(bookingRepository.save(b));
    }

    @Override
    public SalonBookingDto cancelMine(Long bookingId, ApplicationUser user) {
        SalonBooking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable: " + bookingId));
        Client client = user != null ? user.getClient() : null;
        if (client == null || b.getClient() == null || !b.getClient().getId().equals(client.getId())) {
            throw new BusinessException("Vous ne pouvez annuler que vos propres réservations");
        }
        if (b.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Une réservation terminée ne peut pas être annulée");
        }
        b.setStatus(BookingStatus.CANCELLED);
        return toDto(bookingRepository.save(b));
    }

    @Override
    public SalonBookingDto review(Long bookingId,
            com.salahtech.BarberShop_Apis.Dtos.CreateSalonReviewRequest req, ApplicationUser user) {
        SalonBooking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable: " + bookingId));
        Client client = user != null ? user.getClient() : null;
        if (client == null || b.getClient() == null || !b.getClient().getId().equals(client.getId())) {
            throw new BusinessException("Vous ne pouvez noter que vos propres réservations");
        }
        if (b.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessException("Vous pourrez laisser un avis une fois la prestation terminée");
        }
        if (req.getOverallRating() == null || req.getOverallRating() < 1 || req.getOverallRating() > 5) {
            throw new BusinessException("La note doit être comprise entre 1 et 5");
        }
        if (req.getTipAmount() != null && req.getTipAmount().signum() < 0) {
            throw new BusinessException("Le pourboire ne peut pas être négatif");
        }
        if (reviewRepository.existsBySalonBooking_Id(b.getId())) {
            throw new BusinessException("Vous avez déjà laissé un avis pour cette réservation");
        }
        String tagsCsv = (req.getTags() == null || req.getTags().isEmpty()) ? null : String.join(",", req.getTags());
        reviewRepository.save(SalonReview.builder()
                .salonBooking(b)
                .salon(b.getSalon())
                .client(client)
                .overallRating(req.getOverallRating())
                .comment(req.getComment())
                .tags(tagsCsv)
                .tipAmount(req.getTipAmount())
                .build());
        log.info("Avis salon créé pour réservation {} (note {})", b.getId(), req.getOverallRating());
        return toDto(b);
    }

    @Override
    @Transactional(readOnly = true)
    public SalonStatsDto getStats(Long salonId, LocalDate from, LocalDate to) {
        return getStats(salonId, from, to, null);
    }

    @Override
    @Transactional(readOnly = true)
    public SalonStatsDto getStats(Long salonId, LocalDate from, LocalDate to, Long employeeId) {
        salon(salonId);
        LocalDate f = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate t = to != null ? to : LocalDate.now();
        List<SalonBooking> rows = bookingRepository.findBySalon_IdAndStartAtBetweenOrderByStartAtAsc(
                salonId, f.atStartOfDay(), t.plusDays(1).atStartOfDay())
                .stream()
                .filter(b -> employeeId == null
                        || (b.getAssignedEmployee() != null && employeeId.equals(b.getAssignedEmployee().getId())))
                .collect(Collectors.toList());

        long pending = 0, confirmed = 0, completed = 0, cancelled = 0, noShow = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        Map<String, Long> svcCount = new java.util.HashMap<>();
        Map<LocalDate, Long> dayCount = new java.util.TreeMap<>();

        for (SalonBooking b : rows) {
            BookingStatus st = b.getStatus();
            if (st == BookingStatus.PENDING) pending++;
            else if (st == BookingStatus.CONFIRMED || st == BookingStatus.BOOKED) confirmed++;
            else if (st == BookingStatus.COMPLETED) completed++;
            else if (st == BookingStatus.CANCELLED) cancelled++;
            else if (st == BookingStatus.NO_SHOW) noShow++;

            boolean counts = st == BookingStatus.CONFIRMED || st == BookingStatus.BOOKED || st == BookingStatus.COMPLETED;
            if (counts && b.getTotalPrice() != null) revenue = revenue.add(b.getTotalPrice());

            if (st != BookingStatus.CANCELLED) {
                String sv = (b.getOffering() != null && b.getOffering().getName() != null)
                        ? b.getOffering().getName() : "—";
                svcCount.merge(sv, 1L, Long::sum);
                dayCount.merge(b.getStartAt().toLocalDate(), 1L, Long::sum);
            }
        }

        List<SalonStatsDto.NameCount> top = svcCount.entrySet().stream()
                .map(e -> SalonStatsDto.NameCount.builder().name(e.getKey()).count(e.getValue()).build())
                .sorted((a, b2) -> Long.compare(b2.getCount(), a.getCount()))
                .limit(5)
                .collect(Collectors.toList());

        List<SalonStatsDto.DayCount> per = dayCount.entrySet().stream()
                .map(e -> SalonStatsDto.DayCount.builder().date(e.getKey()).count(e.getValue()).build())
                .collect(Collectors.toList());

        return SalonStatsDto.builder()
                .from(f).to(t)
                .total(rows.size())
                .pending(pending).confirmed(confirmed).completed(completed).cancelled(cancelled).noShow(noShow)
                .revenue(revenue)
                .topServices(top).perDay(per)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalonClientDto> listClients(Long salonId, Long employeeId) {
        salon(salonId);
        Map<Long, SalonClientDto> byClient = new java.util.LinkedHashMap<>();
        for (SalonBooking b : bookingRepository.findBySalon_Id(salonId)) {
            if (employeeId != null && (b.getAssignedEmployee() == null
                    || !employeeId.equals(b.getAssignedEmployee().getId()))) {
                continue; // prestataire : seulement ses propres clients
            }
            Client c = b.getClient();
            if (c == null) continue;
            SalonClientDto dto = byClient.computeIfAbsent(c.getId(), id -> SalonClientDto.builder()
                    .clientId(c.getId())
                    .name(((c.getFirstName() != null ? c.getFirstName() : "") + " "
                            + (c.getLastName() != null ? c.getLastName() : "")).trim())
                    .email(c.getEmail())
                    .phone(c.getPhone())
                    .totalBookings(0)
                    .totalSpent(BigDecimal.ZERO)
                    .build());

            if (b.getStatus() != BookingStatus.CANCELLED) {
                dto.setTotalBookings(dto.getTotalBookings() + 1);
                if (b.getStartAt() != null && (dto.getLastVisit() == null || b.getStartAt().isAfter(dto.getLastVisit()))) {
                    dto.setLastVisit(b.getStartAt());
                }
            }
            boolean counts = b.getStatus() == BookingStatus.CONFIRMED
                    || b.getStatus() == BookingStatus.BOOKED
                    || b.getStatus() == BookingStatus.COMPLETED;
            if (counts && b.getTotalPrice() != null) {
                dto.setTotalSpent(dto.getTotalSpent().add(b.getTotalPrice()));
            }
        }
        return byClient.values().stream()
                .sorted((a, b) -> {
                    LocalDateTime la = a.getLastVisit(), lb = b.getLastVisit();
                    if (la == null && lb == null) return 0;
                    if (la == null) return 1;
                    if (lb == null) return -1;
                    return lb.compareTo(la); // plus récent d'abord
                })
                .collect(Collectors.toList());
    }

    private SalonBooking owned(Long salonId, Long bookingId) {
        SalonBooking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable: " + bookingId));
        if (b.getSalon() == null || !b.getSalon().getId().equals(salonId)) {
            throw new BusinessException("Cette réservation n'appartient pas au salon " + salonId);
        }
        return b;
    }

    private SalonBookingDto toDto(SalonBooking b) {
        Client c = b.getClient();
        String clientName = c == null ? null
                : ((c.getFirstName() != null ? c.getFirstName() : "") + " "
                + (c.getLastName() != null ? c.getLastName() : "")).trim();
        return SalonBookingDto.builder()
                .id(b.getId())
                .salonId(b.getSalon() != null ? b.getSalon().getId() : null)
                .salonName(b.getSalon() != null ? b.getSalon().getName() : null)
                .serviceId(b.getOffering() != null ? b.getOffering().getId() : null)
                .serviceName(b.getOffering() != null ? b.getOffering().getName() : null)
                .durationMinutes(b.getOffering() != null ? b.getOffering().getDurationMinutes() : null)
                .clientId(c != null ? c.getId() : null)
                .clientName(clientName)
                .startAt(b.getStartAt())
                .endAt(b.getEndAt())
                .status(b.getStatus() != null ? b.getStatus().name() : null)
                .totalPrice(b.getTotalPrice())
                .workstationId(b.getAssignedWorkstation() != null ? b.getAssignedWorkstation().getId() : null)
                .workstationName(b.getAssignedWorkstation() != null ? b.getAssignedWorkstation().getName() : null)
                .employeeId(b.getAssignedEmployee() != null ? b.getAssignedEmployee().getId() : null)
                .employeeName(b.getAssignedEmployee() != null
                        ? (((b.getAssignedEmployee().getFirstName() != null ? b.getAssignedEmployee().getFirstName() : "") + " "
                           + (b.getAssignedEmployee().getLastName() != null ? b.getAssignedEmployee().getLastName() : "")).trim())
                        : null)
                .notes(b.getNotes())
                .reviewed(b.getId() != null && reviewRepository.existsBySalonBooking_Id(b.getId()))
                .build();
    }
}
