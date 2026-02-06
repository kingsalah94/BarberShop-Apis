package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.AOP.LogActivity;
import com.salahtech.BarberShop_Apis.Dtos.AvailabilityDto;
import com.salahtech.BarberShop_Apis.Dtos.ExceptionTimeSlotDto;
import com.salahtech.BarberShop_Apis.Dtos.TimeSlotDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Enums.ErrorCodes;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.AvailabilityService;
import com.salahtech.BarberShop_Apis.models.Availability;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.reppsitories.AvailabilityRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BarberRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AvailabilityServiceImpl implements AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final BookingRepository bookingRepository;
    private final BarberRepository barberRepository;

    /** Horaires par défaut si Barber.workingHours est absent ou invalide */
    // private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    // private static final LocalTime DEFAULT_END_TIME = LocalTime.of(00, 0);
    private static final int DEFAULT_SLOT_DURATION = 30; // minutes
    private static final int STEP_MINUTES = 30; // pas d’itération
    private static final ZoneId ZONE = ZoneId.of("Africa/Niamey");
    // private static final int STEP_MINUTES = 15;

    // petite classe interne au lieu d'un record local
    private static final class Window {
        final LocalDateTime start;
        final LocalDateTime end;

        Window(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
    }

    // helper pour arrondir au prochain pas
    private static LocalDateTime roundUp(LocalDateTime t, int stepMinutes) {
        int r = t.getMinute() % stepMinutes;
        if (r != 0)
            t = t.plusMinutes(stepMinutes - r);
        return t.withSecond(0).withNano(0);
    }

    // -------------------- CRUD de base --------------------

    // Sauvegarder une disponibilité
    @Override
    @LogActivity(action = "CREATE_AVAILABILITY", entityName = "Availability")
    public AvailabilityDto save(AvailabilityDto dto) {
        if (dto == null || dto.getBarberId() == null) {
            throw new InvalideEntityException("Availability invalide (barberId manquant)",
                    ErrorCodes.INVALID_ENTITY);
        }

        // vérifie le barber
        Barber barber = barberRepository.findById(dto.getBarberId())
                .orElseThrow(() -> new ResourceNotFoundException("Barber introuvable: " + dto.getBarberId()));

        // map DTO -> entity en fixant le barber
        Availability availability = AvailabilityDto.toEntity(dto);
        availability.setBarber(barber);

        // la validation fine est déjà dans @PrePersist/@PreUpdate de l’entité (XOR
        // date/dayOfWeek)
        availability = availabilityRepository.save(availability);
        return AvailabilityDto.fromEntity(availability);
    }

    // Trouver une disponibilité par ID
    @Override
    @Transactional(readOnly = true)
    public AvailabilityDto findById(Long id) {
        return availabilityRepository.findById(id)
                .map(AvailabilityDto::fromEntity)
                .orElseThrow(() -> new InvalideEntityException(
                        "Aucune disponibilité trouvée avec l'ID " + id,
                        ErrorCodes.AVAILABILITY_NOT_FOUND));
    }

    // Liste de toutes les disponibilités
    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityDto> findAll() {
        return availabilityRepository.findAll().stream()
                .map(AvailabilityDto::fromEntity)
                .collect(Collectors.toList());
    }

    // Supprimer une disponibilité par ID
    @Override
    @LogActivity(action = "DELETE_AVAILABILITY", entityName = "Availability")
    public void delete(Long id) {
        if (id == null) {
            log.warn("delete(null) ignoré");
            return;
        }
        availabilityRepository.deleteById(id);
    }

    // -------------------- Métiers --------------------

    @Override
    public List<AvailabilityDto> getByBarber(Long barberId) {
        if (barberId == null || barberId <= 0) {
            throw new IllegalArgumentException("barberId est requis");
        }
        log.debug("Listing availabilities for barberId={}", barberId);

        // essaie la version triée si tu l’as déclarée, sinon fallback
        List<Availability> list;
        try {
            list = availabilityRepository.findAllByBarberSorted(barberId);
            if (list == null || list.isEmpty()) {
                list = availabilityRepository.findByBarber_Id(barberId);
                if (list == null || list.isEmpty()) {
                    list = availabilityRepository.findByBarberId(barberId);
                }
            }
        } catch (Exception e) {
            // En cas d’entité sans relation, fallback sur le champ à plat
            list = availabilityRepository.findByBarberId(barberId);
        }

        return list.stream()
                .map(AvailabilityDto::fromEntity) // <-- tes mappings manuels
                .toList();
    }

    // AvailabilityServiceImpl.java (extrait)
    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotDto> getAvailableSlots(Long barberId, LocalDate date, Integer serviceDuration) {
        log.info("Recherche des créneaux disponibles pour barber={} le {} (durée={})", barberId, date, serviceDuration);

        // garde-fous
        if (barberId == null || date == null || serviceDuration == null || serviceDuration < 1) {
            log.warn("Paramètres invalides: barberId={}, date={}, serviceDuration={}", barberId, date, serviceDuration);
            return List.of();
        }
        // ne pas proposer dans le passé
        if (date.isBefore(LocalDate.now(ZONE)))
            return List.of();

        final Barber barber = barberRepository.findById(barberId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barbier introuvable: " + barberId));
        if (Boolean.FALSE.equals(barber.getAvailable()))
            return List.of();

        final DayOfWeek dow = date.getDayOfWeek();
        final LocalDateTime startOfDay = date.atStartOfDay();
        final LocalDateTime endOfDay = date.atTime(23, 59, 59);

        // ✅ nouvelles signatures repo (champ relationnel "barber" + date LocalDate)
        final List<Availability> recurring = availabilityRepository.findByBarberIdAndDayOfWeek(barberId, dow);
        final List<Availability> oneOff = availabilityRepository.findByBarberIdAndDate(barberId, date);

        final List<Availability> dayAvailabilities = new ArrayList<>(recurring.size() + oneOff.size());
        dayAvailabilities.addAll(recurring);
        dayAvailabilities.addAll(oneOff);
        if (dayAvailabilities.isEmpty())
            return List.of();

        // bookings existants du jour (on ignore CANCELLED)
        final List<Booking> busy = bookingRepository.findConflictingBookings(barberId, startOfDay, endOfDay)
                .stream()
                .filter(b -> {
                    final var st = b.getStatus();
                    return st == null || st == BookingStatus.PENDING || st == BookingStatus.CONFIRMED;
                })
                .toList();

        // construire les fenêtres (start/end) du jour
        final List<Window> windows = new ArrayList<>();
        for (Availability a : dayAvailabilities) {
            if (a.getIsAvailable() != null && !a.getIsAvailable())
                continue;

            final LocalTime s = a.getStartAt();
            final LocalTime e = a.getEndAt();
            if (s == null || e == null || !e.isAfter(s))
                continue;

            final LocalDate windowDate = (a.getDate() != null ? a.getDate() : date);
            final LocalDateTime wStart = LocalDateTime.of(windowDate, s);
            final LocalDateTime wEnd = LocalDateTime.of(windowDate, e);

            final LocalDateTime start = wStart.isBefore(startOfDay) ? startOfDay : wStart;
            final LocalDateTime end = wEnd.isAfter(endOfDay) ? endOfDay : wEnd;
            if (end.isAfter(start))
                windows.add(new Window(start, end));
        }
        if (windows.isEmpty())
            return List.of();

        // merge fenêtres qui se chevauchent
        windows.sort(Comparator.comparing(w -> w.start));
        final List<Window> merged = new ArrayList<>();
        for (Window w : windows) {
            if (merged.isEmpty()) {
                merged.add(w);
            } else {
                Window last = merged.get(merged.size() - 1);
                if (!w.start.isAfter(last.end)) {
                    merged.set(merged.size() - 1, new Window(last.start, last.end.isAfter(w.end) ? last.end : w.end));
                } else {
                    merged.add(w);
                }
            }
        }

        // filtrage "aujourd’hui" : pas de slots passés
        final boolean isToday = date.equals(LocalDate.now(ZONE));
        final LocalDateTime now = LocalDateTime.now(ZONE);

        final List<TimeSlotDto> result = new ArrayList<>();
        for (Window w : merged) {
            LocalDateTime slotStart = w.start;

            if (isToday && now.isAfter(slotStart)) {
                slotStart = roundUp(now, STEP_MINUTES);
                if (slotStart.isAfter(w.end))
                    continue;
            }

            while (true) {
                LocalDateTime slotEnd = slotStart.plusMinutes(serviceDuration);
                if (slotEnd.isAfter(w.end))
                    break;

                final LocalDateTime checkStart = slotStart;
                final LocalDateTime checkEnd = slotEnd;

                final boolean overlapsBooking = busy.stream().anyMatch(b -> {
                    final LocalDateTime bStart = (b.getStartAt() != null ? b.getStartAt() : b.getBookingDate());
                    final LocalDateTime bEnd = b.getEndAt();
                    return bStart != null && bEnd != null
                            && checkStart.isBefore(bEnd)
                            && checkEnd.isAfter(bStart);
                });

                if (!overlapsBooking) {
                    result.add(TimeSlotDto.builder()
                            .startAt(checkStart)
                            .endAt(checkEnd)
                            .available(true)
                            .build());
                }
                slotStart = slotStart.plusMinutes(STEP_MINUTES);
            }
        }

        // dé-duplication
        if (result.size() > 1) {
            final Map<String, TimeSlotDto> uniq = new LinkedHashMap<>();
            for (TimeSlotDto t : result)
                uniq.putIfAbsent(t.getStartAt() + "|" + t.getEndAt(), t);
            log.info("Trouvé {} créneaux disponibles (après merge)", uniq.size());
            return new ArrayList<>(uniq.values());
        }
        log.info("Trouvé {} créneaux disponibles", result.size());
        return result;
    }

    // Vérifier si un créneau est disponible

    @Override
    @Transactional(readOnly = true)
    public boolean isSlotAvailable(Long barberId, LocalDateTime startAt, LocalDateTime endAt) {
        if (barberId == null || startAt == null || endAt == null || !endAt.isAfter(startAt))
            return false;

        // 1) Le créneau est-il dans une dispo (récurrente OU ponctuelle) ?
        DayOfWeek dow = startAt.getDayOfWeek();
        LocalDate date = startAt.toLocalDate();

        List<Availability> recurring = availabilityRepository.findByBarberIdAndDayOfWeek(barberId, dow);
        List<Availability> oneOff = availabilityRepository.findByBarberIdAndDateBetween(
                barberId, date.atStartOfDay(), date.atTime(23, 59, 59));

        boolean within = false;
        for (Availability a : concat(recurring, oneOff)) {
            LocalTime s = a.getStartAt(), e = a.getEndAt();
            if (s != null && e != null
                    && !startAt.toLocalTime().isBefore(s)
                    && !endAt.toLocalTime().isAfter(e)) {
                within = true;
                break;
            }
        }
        if (!within)
            return false;

        // 2) Conflits ?
        return bookingRepository.findConflictingBookings(barberId, startAt, endAt).isEmpty();
    }

    // // Conflit entre deux intervalles [aStart,aEnd[ et [bStart,bEnd[ ?
    // private boolean overlap(LocalDateTime aStart, LocalDateTime aEnd,
    // LocalDateTime bStart, LocalDateTime bEnd) {
    // return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    // }

    // Méthode helper pour vérifier le chevauchement
    private boolean overlaps(LocalDateTime start1, LocalDateTime end1,
            LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    // Concaténation null-safe de deux listes
    private LocalDateTime startOfDay(LocalDate d) {
        return d.atTime(LocalTime.MIN);
    }

    // 23:59:59.999999999
    private LocalDateTime endOfDay(LocalDate d) {
        return d.atTime(LocalTime.MAX);
    }

    // Simple concaténation de deux listes (null-safe)

    @Override
    @Transactional
    @LogActivity(action = "BLOCK_TIME_SLOT", entityName = "Availability")
    public void blockTimeSlot(Long barberId, LocalDateTime startAt, LocalDateTime endAt, String reason) {
        log.info("Blocage demandé pour barber={} [{} - {}] reason={}", barberId, startAt, endAt, reason);

        if (barberId == null || startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new InvalideEntityException(
                    "Paramètres invalides pour le blocage de créneau",
                    ErrorCodes.BAD_REQUEST);
        }

        // Vérifier que le barbier existe
        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barber introuvable: " + barberId));

        // Créer une Availability “négative” (ponctuelle) pour ce jour
        Availability block = Availability.builder()
                .barber(barber)
                .date(startAt.toLocalDate()) // mode ponctuel (XOR dayOfWeek)
                .dayOfWeek(null)
                .startAt(startAt.toLocalTime())
                .endAt(endAt.toLocalTime())
                .isAvailable(false)
                .note(reason != null ? ("BLOCKED: " + reason) : "BLOCKED")
                .build();

        // La validation @PrePersist/@PreUpdate de Availability va faire:
        // - XOR date/dayOfWeek
        // - vérifier start<end
        availabilityRepository.save(block);
        log.info("Créneau bloqué enregistré (availability.id={})", block.getId());
    }

    // Libérer un créneau bloqué
    // On supprime les Availability isAvailable=false qui chevauchent le créneau

    @Override
    @Transactional
    @LogActivity(action = "RELEASE_TIME_SLOT", entityName = "Availability")
    public void releaseTimeSlot(Long barberId, LocalDateTime startAt, LocalDateTime endAt) {
        log.info("Libération demandée pour barber={} [{} - {}]", barberId, startAt, endAt);

        if (barberId == null || startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new InvalideEntityException(
                    "Paramètres invalides pour la libération de créneau",
                    ErrorCodes.BAD_REQUEST);
        }

        // Récupère tous les “blocks” (isAvailable=false) du jour concerné
        LocalDate day = startAt.toLocalDate();
        List<Availability> candidates = availabilityRepository
                .findByBarberIdAndDateBetween(barberId, startOfDay(day), endOfDay(day))
                .stream()
                .filter(a -> Boolean.FALSE.equals(a.getIsAvailable()))
                .filter(a -> overlaps(
                        day.atTime(a.getStartAt()),
                        day.atTime(a.getEndAt()),
                        startAt, endAt))
                .toList();

        if (candidates.isEmpty()) {
            log.info("Aucun blocage correspondant à libérer.");
            return;
        }

        availabilityRepository.deleteAll(candidates);
        log.info("Libérés {} blocage(s) pour le barbier {}", candidates.size(), barberId);
    }

    // Obtenir la disponibilité hebdo d’un barbier
    // On retourne toutes les dispos (récurrentes + ponctuelles) du barbier

    @Override
    @Transactional
    public List<AvailabilityDto> getWeeklyAvailability(Long barberId, LocalDate weekStart) {
        // ici on retourne toutes les dispos du barber; tu peux filtrer la semaine si tu
        // veux
        return availabilityRepository.findByBarber_Id(barberId).stream()
                .map(AvailabilityDto::fromEntity)
                .collect(Collectors.toList());
    }

    // Mise à jour de l’horaire hebdo d’un barbier
    // On remplace toutes les dispos existantes par celles fournies

    // Dans AvailabilityServiceImpl.java

    @Override
    @Transactional
    @LogActivity(action = "UPDATE_WEEKLY_SCHEDULE", entityName = "BarberSchedule")
    public void updateWeeklySchedule(Long barberId, List<AvailabilityDto> weeklySchedule) {
        log.info("Mise à jour de l'horaire hebdo pour barber={}, reçu {} créneaux",
                barberId, weeklySchedule != null ? weeklySchedule.size() : 0);

        // 1. Vérifier que le barbier existe
        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Barbier non trouvé avec l'ID : " + barberId));

        // 2. Validation des données entrantes
        if (weeklySchedule == null || weeklySchedule.isEmpty()) {
            throw new InvalideEntityException(
                    "L'horaire hebdomadaire ne peut pas être vide. Envoyez au moins un créneau.");
        }

        // 3. Valider chaque AvailabilityDto reçu
        List<AvailabilityDto> invalidDtos = weeklySchedule.stream()
                .filter(dto -> !dto.isValid())
                .toList();

        if (!invalidDtos.isEmpty()) {
            log.error("❌ {} créneaux invalides détectés", invalidDtos.size());
            invalidDtos.forEach(dto -> {
                log.error("  - Créneau invalide: dayOfWeek={}, startAt={}, endAt={}",
                        dto.getDayOfWeek(), dto.getStartAt(), dto.getEndAt());
            });
            throw new InvalideEntityException(
                    String.format("L'horaire contient %d créneau(x) invalide(s). " +
                            "Vérifiez que chaque créneau récurrent a startAt et endAt définis.",
                            invalidDtos.size()));
        }

        // 4. Supprimer tous les créneaux récurrents existants (où date == null)
        List<Availability> existing = availabilityRepository.findByBarberIdAndDateIsNull(barberId);
        if (!existing.isEmpty()) {
            log.info("Suppression de {} créneaux récurrents existants", existing.size());
            availabilityRepository.deleteAll(existing);
            availabilityRepository.flush(); // Force la suppression avant insertion
        }

        // 5. Convertir les DTOs en entités et associer le barbier
        List<Availability> newAvailabilities = new ArrayList<>();

        for (AvailabilityDto dto : weeklySchedule) {
            // Ignorer les créneaux ponctuels (avec date) pour cette route
            if (dto.getDate() != null) {
                log.warn("⚠️ Créneau avec date ignoré dans updateWeeklySchedule: {}", dto.getDate());
                continue;
            }

            // Vérifier que c'est bien un créneau récurrent
            if (dto.getDayOfWeek() == null) {
                log.warn("⚠️ Créneau sans dayOfWeek ignoré");
                continue;
            }

            // ✅ LOG DE DEBUG : Afficher les données reçues
            log.debug("📥 Traitement créneau: day={}, startAt={}, endAt={}",
                    dto.getDayOfWeek(), dto.getStartAt(), dto.getEndAt());

            // Vérification finale (redondante mais sécurisée)
            if (dto.getStartAt() == null || dto.getEndAt() == null) {
                log.error("❌ ERREUR CRITIQUE: Créneau {} sans startAt/endAt", dto.getDayOfWeek());
                throw new InvalideEntityException(
                        String.format("Le créneau pour %s doit avoir startAt et endAt définis.",
                                dto.getDayOfWeek()));
            }

            // Conversion DTO -> Entity
            Availability avail = AvailabilityDto.toEntity(dto);

            // Associer le barbier
            avail.setBarber(barber);

            // S'assurer que les champs sont bien définis
            avail.setDate(null); // Récurrent = pas de date
            avail.setIsAvailable(dto.getIsAvailable() != null ? dto.getIsAvailable() : true);

            newAvailabilities.add(avail);
        }

        // 6. Vérifier qu'il reste au moins un créneau après filtrage
        if (newAvailabilities.isEmpty()) {
            throw new InvalideEntityException(
                    "Aucun créneau récurrent valide trouvé après traitement. " +
                            "Vérifiez que les créneaux ont dayOfWeek, startAt et endAt définis.");
        }

        // 7. Sauvegarder les nouveaux créneaux
        log.info("💾 Sauvegarde de {} nouveaux créneaux récurrents", newAvailabilities.size());

        try {
            List<Availability> saved = availabilityRepository.saveAll(newAvailabilities);
            log.info("✅ {} créneaux sauvegardés avec succès", saved.size());
        } catch (Exception e) {
            log.error("❌ Erreur lors de la sauvegarde des créneaux: {}", e.getMessage());
            throw new InvalideEntityException(
                    "Erreur lors de la sauvegarde des créneaux: " + e.getMessage());
        }
    }

    /**
     * ⚠️ MÉTHODE À AJOUTER dans AvailabilityRepository si elle n'existe pas
     */
    // List<Availability> findByBarberIdAndDateIsNull(Long barberId);
    /**
     * Disponibilité d’un barbier pour une date.
     * Combine les dispos récurrentes (DayOfWeek) et ponctuelles (date == jour).
     * Marque les créneaux comme “booked” ou “available” selon les bookings
     * existants.
     */

    @Override
    @Transactional(readOnly = true)
    public AvailabilityDto getBarberAvailability(Long barberId, LocalDate date) {
        log.info("Get availability (rich) for barber={} date={}", barberId, date);

        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barber introuvable: " + barberId));

        if (Boolean.FALSE.equals(barber.getAvailable())) {
            /// return new AvailabilityDto(barberId, date.atStartOfDay(), new ArrayList<>(),
            /// new ArrayList<>());
            return AvailabilityDto.builder()
                    .barberId(barberId)
                    .date(date.atStartOfDay().toLocalDate())
                    .availableSlots(new ArrayList<>())
                    .bookedSlots(new ArrayList<>())
                    .build();

        }

        // bookings du jour pour ce barber
        LocalDateTime startOfDay = date.atTime(LocalTime.MIN);
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Booking> bookings = bookingRepository.findByBookingDateBetween(startOfDay, endOfDay).stream()
                .filter(b -> b.getBarber() != null && Objects.equals(b.getBarber().getId(), barberId))
                .filter(b -> {
                    BookingStatus st = b.getStatus();
                    return st == BookingStatus.CONFIRMED || st == BookingStatus.PENDING;
                })
                .collect(Collectors.toList());

        // slots théoriques selon workingHours
        List<TimeSlotDto> allSlots = generateTimeSlots(date, barber.getWorkingHours());

        // marquer occupés vs libres (bookingDate/startAt → endAt)
        List<TimeSlotDto> available = new ArrayList<>();
        List<TimeSlotDto> booked = new ArrayList<>();

        for (TimeSlotDto slot : allSlots) {
            boolean isBooked = bookings.stream().anyMatch(b -> {
                LocalDateTime bStart = b.getStartAt() != null ? b.getStartAt() : b.getBookingDate();
                LocalDateTime bEnd = b.getEndAt();
                return isTimeSlotOverlapping(slot.getStartAt(), slot.getEndAt(), bStart, bEnd);
            });

            if (isBooked) {
                Booking ov = bookings.stream().filter(b -> {
                    LocalDateTime bStart = b.getStartAt() != null ? b.getStartAt() : b.getBookingDate();
                    LocalDateTime bEnd = b.getEndAt();
                    return isTimeSlotOverlapping(slot.getStartAt(), slot.getEndAt(), bStart, bEnd);
                }).findFirst().orElse(null);

                slot.setAvailable(false);
                slot.setBookingId(ov != null ? ov.getId() : null);
                booked.add(slot);
            } else {
                slot.setAvailable(true);
                available.add(slot);
            }
        }

        // return new AvailabilityDto(barberId, date.atStartOfDay(), available, booked);
        return AvailabilityDto.builder()
                .barberId(barberId)
                .date(date.atStartOfDay().toLocalDate())
                .availableSlots(new ArrayList<>())
                .bookedSlots(new ArrayList<>())
                .build();

    }

    /**
     * Prochains créneaux disponibles pour un barbier.
     * Utile pour la page publique de réservation.
     */

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotDto> getNextAvailableSlots(Long barberId, int numberOfSlots) {
        log.info("Next {} slots for barber={}", numberOfSlots, barberId);

        if (barberId == null || numberOfSlots <= 0)
            return List.of();

        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barber introuvable: " + barberId));
        if (Boolean.FALSE.equals(barber.getAvailable()))
            return List.of();

        final int SLOT_MINUTES = 30; // durée d’un créneau

        List<TimeSlotDto> found = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 30 && found.size() < numberOfSlots; i++) {
            LocalDate date = today.plusDays(i);
            AvailabilityDto day = getBarberAvailability(barberId, date);

            // day.getAvailableSlots() === List<LocalTime>
            List<TimeSlotDto> daily = day.getAvailableSlots().stream()
                    .map(t -> {
                        var start = date.atTime(t);
                        return TimeSlotDto.builder()
                                .startAt(start)
                                .endAt(start.plusMinutes(SLOT_MINUTES))
                                .available(true)
                                .build();
                    })
                    .filter(s -> s.getStartAt().isAfter(LocalDateTime.now()))
                    .limit(numberOfSlots - found.size())
                    .toList();

            found.addAll(daily);
        }
        return found;
    }

    /**
     * Disponibilités de tous les barbiers pour une date.
     * Utile pour l’admin ou la page publique de réservation.
     */

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityDto> getAllBarbersAvailability(LocalDate date) {
        return barberRepository.findByAvailable(true).stream()
                .map(b -> getBarberAvailability(b.getId(), date))
                .collect(Collectors.toList());
    }

    /**
     * Génère des créneaux pour une date selon les horaires de travail.
     * Si workingHours invalide ou absent, utilise les valeurs par défaut.
     * 
     * @param date         jour pour lequel générer les créneaux
     * @param workingHours format "HH:mm-HH:mm" (ex: "09:00-18:00")
     * @return liste de TimeSlotDto couvrant la journée
     */

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotDto> generateTimeSlots(LocalDate date, String workingHours) {
        // 1. Déterminer la plage globale de la journée
        LocalTime start = LocalTime.MIN; // 00:00
        LocalTime end = LocalTime.MAX; // 23:59:59

        // Si le barbier a défini des "Working Hours" globales (ex: "09:00-19:00"), on
        // les respecte
        if (workingHours != null && workingHours.contains("-")) {
            String[] parts = workingHours.split("-");
            if (parts.length == 2) {
                try {
                    start = LocalTime.parse(parts[0].trim());
                    end = LocalTime.parse(parts[1].trim());
                } catch (Exception e) {
                    log.warn("Format workingHours invalide : {}", workingHours);
                }
            }
        }

        List<TimeSlotDto> slots = new ArrayList<>();
        LocalDateTime cur = date.atTime(start);
        LocalDateTime endOfDay = date.atTime(end);

        // 2. Générer les créneaux
        while (cur.plusMinutes(DEFAULT_SLOT_DURATION).isBefore(endOfDay)
                || cur.plusMinutes(DEFAULT_SLOT_DURATION).equals(endOfDay)) {
            LocalDateTime slotEnd = cur.plusMinutes(DEFAULT_SLOT_DURATION);

            slots.add(TimeSlotDto.builder()
                    .startAt(cur)
                    .endAt(slotEnd)
                    .available(true)
                    .build());

            cur = cur.plusMinutes(DEFAULT_SLOT_DURATION);
        }
        return slots;
    }

    // Vérifie si deux créneaux (start1-end1 et start2-end2) se chevauchent

    private boolean isTimeSlotOverlapping(LocalDateTime s1, LocalDateTime e1,
            LocalDateTime s2, LocalDateTime e2) {
        if (s1 == null || e1 == null || s2 == null || e2 == null)
            return false;
        return s1.isBefore(e2) && s2.isBefore(e1);
    }

    /**
     * Statistiques de disponibilité pour un barbier sur une période.
     * Total jours, total créneaux, créneaux réservés, taux d’occupation.
     */

    @Override
    @Transactional(readOnly = true)
    public AvailabilityStatsDTO getAvailabilityStats(Long barberId, LocalDate startDate, LocalDate endDate) {
        log.info("Stats availability barber={} {} -> {}", barberId, startDate, endDate);

        int totalDays = (int) startDate.datesUntil(endDate.plusDays(1)).count();
        int totalSlots = 0;
        int bookedSlots = 0;

        LocalDate d = startDate;
        while (!d.isAfter(endDate)) {
            AvailabilityDto day = getBarberAvailability(barberId, d);
            totalSlots += day.getAvailableSlots().size() + day.getBookedSlots().size();
            bookedSlots += day.getBookedSlots().size();
            d = d.plusDays(1);
        }

        double occupancyRate = totalSlots > 0 ? (double) bookedSlots / totalSlots * 100.0 : 0.0;

        AvailabilityStatsDTO stats = new AvailabilityStatsDTO();
        stats.setBarberId(barberId);
        stats.setStartDate(startDate);
        stats.setEndDate(endDate);
        stats.setTotalDays(totalDays);
        stats.setTotalSlots(totalSlots);
        stats.setBookedSlots(bookedSlots);
        stats.setAvailableSlots(totalSlots - bookedSlots);
        stats.setOccupancyRate(occupancyRate);
        return stats;
    }

    // Helper pour concaténer deux listes
    private static <T> List<T> concat(List<T> a, List<T> b) {
        if (a.isEmpty())
            return b;
        if (b.isEmpty())
            return a;
        List<T> r = new ArrayList<>(a.size() + b.size());
        r.addAll(a);
        r.addAll(b);
        return r;
    }

    // ---- DTO interne identique à ta version ----

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AvailabilityStatsDTO {
        private Long barberId;
        private LocalDate startDate;
        private LocalDate endDate;
        private int totalDays;
        private int totalSlots;
        private int bookedSlots;
        private int availableSlots;
        private double occupancyRate;
    }

    // --------------------------------------------
    /**
     * Vérifie si un créneau est disponible pour un barbier.
     * Un créneau est dispo s'il est dans une disponibilité positive
     * (récurrente OU ponctuelle) ET qu'il n'y a pas de booking en conflit
     * (PENDING/CONFIRMED) ET pas de blocage ponctuel (dispo négative).
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isTimeSlotAvailable(Long barberId, LocalDateTime startAt, LocalDateTime endAt) {
        if (barberId == null || startAt == null || endAt == null || !endAt.isAfter(startAt))
            return false;

        // 1) Conflits avec bookings (PENDING/CONFIRMED)
        boolean hasBookingConflict = !bookingRepository
                .findConflictingBookings(barberId, startAt, endAt).isEmpty();
        if (hasBookingConflict)
            return false;

        // 2) Conflits avec Availability négatives (blocages ponctuels)
        LocalDate day = startAt.toLocalDate();

        // ✅ CORRECTION : Utiliser findByBarber_IdAndDate avec LocalDate
        boolean hasBlock = availabilityRepository
                .findByBarber_IdAndDate(barberId, day) // ← LocalDate au lieu de LocalDateTime
                .stream()
                .filter(a -> Boolean.FALSE.equals(a.getIsAvailable()))
                .anyMatch(a -> {
                    // Reconstruire les heures complètes pour la comparaison
                    LocalDateTime blockStart = day.atTime(a.getStartAt());
                    LocalDateTime blockEnd = day.atTime(a.getEndAt());
                    return overlaps(blockStart, blockEnd, startAt, endAt);
                });

        return !hasBlock;
    }

    // ========= IMPLÉMENTATION DES MÉTHODES EXCEPTIONS =========
    // À ajouter dans AvailabilityServiceImpl.java

    @Override
    @LogActivity(action = "CREATE_EXCEPTION", entityName = "AvailabilityException")
    public AvailabilityDto createException(
            Long barberId,
            LocalDate exceptionDate,
            String exceptionType,
            String reason,
            List<ExceptionTimeSlotDto> customTimeSlots) {

        // Récupérer le barbier
        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barbier non trouvé avec l'id: " + barberId));

        // Créer une nouvelle disponibilité ponctuelle (exception)
        Availability exception = new Availability();
        exception.setBarber(barber);
        exception.setDate(exceptionDate);
        exception.setExceptionType(exceptionType);
        exception.setReason(reason);

        // Si type CLOSED : isAvailable = false
        if ("CLOSED".equalsIgnoreCase(exceptionType)) {
            exception.setIsAvailable(false);
            // startAt et endAt peuvent être null pour CLOSED
            exception.setStartAt(null);
            exception.setEndAt(null);
        }
        // Si type CUSTOM : définir les horaires modifiés
        else if ("CUSTOM".equalsIgnoreCase(exceptionType) && customTimeSlots != null && !customTimeSlots.isEmpty()) {
            exception.setIsAvailable(true);
            // Prendre le premier slot pour l'instant (simplification)
            ExceptionTimeSlotDto firstSlot = customTimeSlots.get(0);
            exception.setStartAt(LocalTime.parse(firstSlot.getStartAt()));
            exception.setEndAt(LocalTime.parse(firstSlot.getEndAt()));
        }

        Availability saved = availabilityRepository.save(exception);
        return AvailabilityDto.fromEntity(saved);
    }

    @Override
    public List<AvailabilityDto> getAllExceptions(Long barberId) {
        // Récupérer toutes les disponibilités avec une date (exceptions)
        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barbier non trouvé avec l'id: " + barberId));

        return availabilityRepository.findByBarberIdAndDateIsNotNull(barberId)
                .stream()
                .map(AvailabilityDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<AvailabilityDto> getUpcomingExceptions(Long barberId) {
        LocalDate today = LocalDate.now();

        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barbier non trouvé avec l'id: " + barberId));

        // Récupérer les exceptions futures
        return availabilityRepository.findByBarberIdAndDateIsNotNull(barberId)
                .stream()
                .filter(a -> a.getDate() != null && !a.getDate().isBefore(today))
                .map(AvailabilityDto::fromEntity)
                .sorted(Comparator.comparing(AvailabilityDto::getDate))
                .collect(Collectors.toList());
    }

    @Override
    public AvailabilityDto getExceptionById(Long id) {
        Availability exception = availabilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exception non trouvée avec l'id: " + id));

        return AvailabilityDto.fromEntity(exception);
    }

    @Override
    @LogActivity(action = "UPDATE_EXCEPTION", entityName = "AvailabilityException")
    public AvailabilityDto updateException(
            Long id,
            LocalDate exceptionDate,
            String exceptionType,
            String reason,
            List<ExceptionTimeSlotDto> customTimeSlots) {

        Availability exception = availabilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exception non trouvée avec l'id: " + id));

        // Mettre à jour les champs fournis
        if (exceptionDate != null) {
            exception.setDate(exceptionDate);
        }

        if (exceptionType != null) {
            exception.setExceptionType(exceptionType);

            if ("CLOSED".equalsIgnoreCase(exceptionType)) {
                exception.setIsAvailable(false);
                exception.setStartAt(null);
                exception.setEndAt(null);
            } else if ("CUSTOM".equalsIgnoreCase(exceptionType) && customTimeSlots != null
                    && !customTimeSlots.isEmpty()) {
                exception.setIsAvailable(true);
                ExceptionTimeSlotDto firstSlot = customTimeSlots.get(0);
                exception.setStartAt(LocalTime.parse(firstSlot.getStartAt()));
                exception.setEndAt(LocalTime.parse(firstSlot.getEndAt()));
            }
        }

        if (reason != null) {
            exception.setReason(reason);
        }

        Availability updated = availabilityRepository.save(exception);
        return AvailabilityDto.fromEntity(updated);
    }

    @Override
    @LogActivity(action = "DELETE_EXCEPTION", entityName = "AvailabilityException")
    public void deleteException(Long id) {
        if (!availabilityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Exception non trouvée avec l'id: " + id);
        }
        availabilityRepository.deleteById(id);
    }

    @Override
    public List<AvailabilityDto> getExceptionsByDateRange(
            Long barberId,
            LocalDate startDate,
            LocalDate endDate) {

        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barbier non trouvé avec l'id: " + barberId));

        return availabilityRepository.findByBarberIdAndDateIsNotNull(barberId)
                .stream()
                .filter(a -> {
                    LocalDate date = a.getDate();
                    return date != null &&
                            !date.isBefore(startDate) &&
                            !date.isAfter(endDate);
                })
                .map(AvailabilityDto::fromEntity)
                .sorted(Comparator.comparing(AvailabilityDto::getDate))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isShopOpenNow(Long barberId) {
        // 1. On récupère l'heure locale exacte (Niamey)
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Africa/Niamey"));
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek currentDay = today.getDayOfWeek();

        // 2. Priorité aux EXCEPTIONS (Ponctuel)
        // On cherche s'il y a une règle spécifique pour cette date précise
        List<Availability> exceptions = availabilityRepository.findByBarberIdAndDate(barberId, today);

        if (!exceptions.isEmpty()) {
            // S'il y a une exception "CLOSED" ou non disponible, c'est fermé.
            boolean isExplicitlyClosed = exceptions.stream()
                    .anyMatch(e -> Boolean.FALSE.equals(e.getIsAvailable())
                            || "CLOSED".equalsIgnoreCase(e.getExceptionType()));

            if (isExplicitlyClosed)
                return false;

            // Sinon (Custom hours), on vérifie si l'heure actuelle matche
            return exceptions.stream()
                    .filter(e -> Boolean.TRUE.equals(e.getIsAvailable()))
                    .anyMatch(e -> isTimeInSlot(currentTime, e.getStartAt(), e.getEndAt()));
        }

        // 3. Sinon, on regarde le PLANNING HEBDO (Récurrent)
        List<Availability> weeklySlots = availabilityRepository.findByBarberIdAndDayOfWeek(barberId, currentDay);

        return weeklySlots.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsAvailable())) // Seulement les slots ouverts
                .anyMatch(e -> isTimeInSlot(currentTime, e.getStartAt(), e.getEndAt()));
    }

    // Petit helper privé pour éviter de répéter la comparaison
    private boolean isTimeInSlot(LocalTime current, LocalTime start, LocalTime end) {
        if (start == null || end == null)
            return false;
        // On inclut le début, on exclut la fin stricte (ex: à 18h00 pile on ferme)
        return !current.isBefore(start) && current.isBefore(end);
    }
}
