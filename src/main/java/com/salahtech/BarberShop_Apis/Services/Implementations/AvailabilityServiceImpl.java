package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.AvailabilityDto;
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
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(18, 0);
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

    /**
     * Créneaux disponibles pour un barbier/date/durée de service.
     * Combine les dispos récurrentes (DayOfWeek) et ponctuelles (date == jour).
     */
    // @Override
    // @Transactional(readOnly = true)
    // public List<TimeSlotDto> getAvailableSlots(Long barberId, LocalDate date,
    // Integer serviceDuration) {
    // log.info("Recherche des créneaux disponibles pour barber={} le {}
    // (durée={})",
    // barberId, date, serviceDuration);

    // // 0) Validation
    // if (barberId == null || date == null || serviceDuration == null ||
    // serviceDuration < 1) {
    // log.warn("Paramètres invalides: barberId={}, date={}, serviceDuration={}",
    // barberId, date, serviceDuration);
    // return List.of();
    // }

    // // 1) Barber
    // final Barber barber = barberRepository.findById(barberId)
    // .orElseThrow(() -> new ResponseStatusException(
    // HttpStatus.NOT_FOUND, "Barbier introuvable: " + barberId));
    // if (Boolean.FALSE.equals(barber.getAvailable())) {
    // return List.of();
    // }

    // // 2) Contexte jour
    // final DayOfWeek dow = date.getDayOfWeek();
    // final LocalDateTime startOfDay = date.atStartOfDay();
    // final LocalDateTime endOfDay = date.atTime(LocalTime.MAX); //
    // 23:59:59.999999999

    // // 2bis) Récupérer les dispos du jour
    // //
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    // // Si ton repo est défini avec un champ "barberId" à plat :
    // final List<Availability> recurring = availabilityRepository
    // .findByBarberIdAndDayOfWeek(barberId, dow);
    // final List<Availability> oneOff = availabilityRepository
    // .findByBarberIdAndDate(barberId, date);

    // // Si au contraire ton entité a ManyToOne barber (champ "barber"), utilise
    // ceci
    // // :
    // // final List<Availability> recurring = availabilityRepository
    // // .findByBarber_IdAndDayOfWeek(barberId, dow);
    // // final List<Availability> oneOff = availabilityRepository
    // // .findByBarber_IdAndDate(barberId, date);
    // //
    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    // final List<Availability> dayAvailabilities = new ArrayList<>(recurring.size()
    // + oneOff.size());
    // dayAvailabilities.addAll(recurring);
    // dayAvailabilities.addAll(oneOff);

    // if (dayAvailabilities.isEmpty()) {
    // log.info("Aucune disponibilité pour barber={} le {}", barberId, date);
    // return List.of();
    // }

    // // 3) Bookings de la journée (on ignore CANCELLED)
    // final List<Booking> busy =
    // bookingRepository.findConflictingBookings(barberId, startOfDay, endOfDay)
    // .stream()
    // .filter(b -> {
    // final BookingStatus st = b.getStatus();
    // return st == null || st == BookingStatus.PENDING || st ==
    // BookingStatus.CONFIRMED;
    // })
    // .toList();

    // // 4) Construire les fenêtres (start/end en LocalDateTime)
    // record Window(LocalDateTime start, LocalDateTime end) {
    // }
    // final List<Window> windows = new ArrayList<>();

    // for (Availability a : dayAvailabilities) {
    // if (a.getIsAvailable() != null && !a.getIsAvailable())
    // continue;

    // final LocalTime s = a.getStartTime();
    // final LocalTime e = a.getEndTime();
    // if (s == null || e == null || !e.isAfter(s))
    // continue;

    // final LocalDate windowDate = (a.getDate() != null ? a.getDate() : date);
    // final LocalDateTime wStart = LocalDateTime.of(windowDate, s);
    // final LocalDateTime wEnd = LocalDateTime.of(windowDate, e);

    // final LocalDateTime start = wStart.isBefore(startOfDay) ? startOfDay :
    // wStart;
    // final LocalDateTime end = wEnd.isAfter(endOfDay) ? endOfDay : wEnd;
    // if (end.isAfter(start)) {
    // windows.add(new Window(start, end));
    // }
    // }
    // if (windows.isEmpty())
    // return List.of();

    // // 5) Fusion des fenêtres qui se chevauchent
    // windows.sort(Comparator.comparing(Window::start));
    // final List<Window> merged = new ArrayList<>();
    // for (Window w : windows) {
    // if (merged.isEmpty()) {
    // merged.add(w);
    // } else {
    // Window last = merged.get(merged.size() - 1);
    // if (!w.start().isAfter(last.end())) { // chevauch./contigu
    // merged.set(merged.size() - 1,
    // new Window(last.start(), last.end().isAfter(w.end()) ? last.end() :
    // w.end()));
    // } else {
    // merged.add(w);
    // }
    // }
    // }

    // // 6) Générer les slots glissants
    // final int STEP_MINUTES = 15;
    // final List<TimeSlotDto> result = new ArrayList<>();

    // for (Window w : merged) {
    // LocalDateTime slotStart = w.start();
    // while (true) {
    // LocalDateTime slotEnd = slotStart.plusMinutes(serviceDuration);
    // if (slotEnd.isAfter(w.end()))
    // break;

    // final LocalDateTime checkStart = slotStart;
    // final LocalDateTime checkEnd = slotEnd;

    // final boolean overlapsBooking = busy.stream().anyMatch(b -> {
    // final LocalDateTime bStart = (b.getStartAt() != null ? b.getStartAt() :
    // b.getBookingDate());
    // final LocalDateTime bEnd = b.getEndAt();
    // return bStart != null && bEnd != null
    // && checkStart.isBefore(bEnd)
    // && checkEnd.isAfter(bStart);
    // });

    // if (!overlapsBooking) {
    // result.add(TimeSlotDto.builder()
    // .startAt(checkStart)
    // .endAt(checkEnd)
    // .available(true)
    // .build());
    // }

    // slotStart = slotStart.plusMinutes(STEP_MINUTES);
    // }
    // }

    // // 7) Dedup
    // if (result.size() > 1) {
    // final Map<String, TimeSlotDto> uniq = new LinkedHashMap<>();
    // for (TimeSlotDto t : result) {
    // String key = t.getStartAt() + "|" + t.getEndAt();
    // uniq.putIfAbsent(key, t);
    // }
    // log.info("Trouvé {} créneaux disponibles (après merge)", uniq.size());
    // return new ArrayList<>(uniq.values());
    // }

    // log.info("Trouvé {} créneaux disponibles", result.size());
    // return result;
    // }

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

            final LocalTime s = a.getStartTime();
            final LocalTime e = a.getEndTime();
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
    public boolean isSlotAvailable(Long barberId, LocalDateTime startTime, LocalDateTime endTime) {
        if (barberId == null || startTime == null || endTime == null || !endTime.isAfter(startTime))
            return false;

        // 1) Le créneau est-il dans une dispo (récurrente OU ponctuelle) ?
        DayOfWeek dow = startTime.getDayOfWeek();
        LocalDate date = startTime.toLocalDate();

        List<Availability> recurring = availabilityRepository.findByBarberIdAndDayOfWeek(barberId, dow);
        List<Availability> oneOff = availabilityRepository.findByBarberIdAndDateBetween(
                barberId, date.atStartOfDay(), date.atTime(23, 59, 59));

        boolean within = false;
        for (Availability a : concat(recurring, oneOff)) {
            LocalTime s = a.getStartTime(), e = a.getEndTime();
            if (s != null && e != null
                    && !startTime.toLocalTime().isBefore(s)
                    && !endTime.toLocalTime().isAfter(e)) {
                within = true;
                break;
            }
        }
        if (!within)
            return false;

        // 2) Conflits ?
        return bookingRepository.findConflictingBookings(barberId, startTime, endTime).isEmpty();
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
    public void blockTimeSlot(Long barberId, LocalDateTime startTime, LocalDateTime endTime, String reason) {
        log.info("Blocage demandé pour barber={} [{} - {}] reason={}", barberId, startTime, endTime, reason);

        if (barberId == null || startTime == null || endTime == null || !endTime.isAfter(startTime)) {
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
                .date(startTime.toLocalDate()) // mode ponctuel (XOR dayOfWeek)
                .dayOfWeek(null)
                .startTime(startTime.toLocalTime())
                .endTime(endTime.toLocalTime())
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
    public void releaseTimeSlot(Long barberId, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("Libération demandée pour barber={} [{} - {}]", barberId, startTime, endTime);

        if (barberId == null || startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new InvalideEntityException(
                    "Paramètres invalides pour la libération de créneau",
                    ErrorCodes.BAD_REQUEST);
        }

        // Récupère tous les “blocks” (isAvailable=false) du jour concerné
        LocalDate day = startTime.toLocalDate();
        List<Availability> candidates = availabilityRepository
                .findByBarberIdAndDateBetween(barberId, startOfDay(day), endOfDay(day))
                .stream()
                .filter(a -> Boolean.FALSE.equals(a.getIsAvailable()))
                .filter(a -> overlaps(
                        day.atTime(a.getStartTime()),
                        day.atTime(a.getEndTime()),
                        startTime, endTime))
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

    @Override
    public void updateWeeklySchedule(Long barberId, List<AvailabilityDto> weeklySchedule) {
        log.info("Mise à jour de l'horaire hebdo pour barber={}", barberId);
        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barber introuvable: " + barberId));

        // purge
        availabilityRepository.deleteByBarber_Id(barberId);

        // map & save
        List<Availability> availabilities = weeklySchedule.stream()
                .map(dto -> {
                    Availability a = AvailabilityDto.toEntity(dto);
                    a.setBarber(barber); // << éviter NPE (tu faisais a.getBarber().setId(...))
                    return a;
                })
                .collect(Collectors.toList());

        availabilityRepository.saveAll(availabilities);
    }

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
        // workingHours attendu "HH:mm-HH:mm". Sinon défaut.
        LocalTime start = DEFAULT_START_TIME;
        LocalTime end = DEFAULT_END_TIME;

        if (workingHours != null && workingHours.contains("-")) {
            String[] parts = workingHours.split("-");
            if (parts.length == 2) {
                try {
                    start = LocalTime.parse(parts[0].trim());
                    end = LocalTime.parse(parts[1].trim());
                } catch (Exception e) {
                    log.warn("Parsing workingHours échoué: {} -> on utilise les valeurs par défaut", workingHours);
                }
            }
        }

        List<TimeSlotDto> slots = new ArrayList<>();
        LocalDateTime cur = date.atTime(start);
        LocalDateTime endOfDay = date.atTime(end);

        while (cur.isBefore(endOfDay)) {
            LocalDateTime slotEnd = cur.plusMinutes(DEFAULT_SLOT_DURATION);
            if (slotEnd.isAfter(endOfDay))
                break;

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
    public boolean isTimeSlotAvailable(Long barberId, LocalDateTime startTime, LocalDateTime endTime) {
        if (barberId == null || startTime == null || endTime == null || !endTime.isAfter(startTime))
            return false;

        // 1) Conflits avec bookings (PENDING/CONFIRMED)
        boolean hasBookingConflict = !bookingRepository
                .findConflictingBookings(barberId, startTime, endTime).isEmpty();
        if (hasBookingConflict)
            return false;

        // 2) Conflits avec Availability négatives (blocages ponctuels)
        LocalDate day = startTime.toLocalDate();

        // ✅ CORRECTION : Utiliser findByBarber_IdAndDate avec LocalDate
        boolean hasBlock = availabilityRepository
                .findByBarber_IdAndDate(barberId, day) // ← LocalDate au lieu de LocalDateTime
                .stream()
                .filter(a -> Boolean.FALSE.equals(a.getIsAvailable()))
                .anyMatch(a -> {
                    // Reconstruire les heures complètes pour la comparaison
                    LocalDateTime blockStart = day.atTime(a.getStartTime());
                    LocalDateTime blockEnd = day.atTime(a.getEndTime());
                    return overlaps(blockStart, blockEnd, startTime, endTime);
                });

        return !hasBlock;
    }

}
