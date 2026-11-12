package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.EffectiveHoursDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonExceptionDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonHoursDto;
import com.salahtech.BarberShop_Apis.Dtos.TimeWindowDto;
import com.salahtech.BarberShop_Apis.Exceptions.BusinessException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonHoursService;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.models.SalonException;
import com.salahtech.BarberShop_Apis.models.SalonHours;
import com.salahtech.BarberShop_Apis.repositories.SalonExceptionRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonHoursRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SalonHoursServiceImpl implements SalonHoursService {

    private final SalonRepository salonRepository;
    private final SalonHoursRepository hoursRepository;
    private final SalonExceptionRepository exceptionRepository;

    private Salon salon(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable: " + salonId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalonHoursDto> getWeeklyHours(Long salonId) {
        salon(salonId); // existence
        Map<DayOfWeek, List<SalonHours>> byDay = new EnumMap<>(DayOfWeek.class);
        hoursRepository.findBySalon_Id(salonId).forEach(h ->
                byDay.computeIfAbsent(h.getDayOfWeek(), k -> new ArrayList<>()).add(h));

        // Toujours les 7 jours dans l'ordre lundi → dimanche.
        return java.util.Arrays.stream(DayOfWeek.values())
                .map(d -> toDayDto(d, byDay.get(d)))
                .collect(Collectors.toList());
    }

    /** Construit le DTO d'un jour à partir de ses lignes (plages). */
    private SalonHoursDto toDayDto(DayOfWeek day, List<SalonHours> rows) {
        List<TimeWindowDto> windows = toWindows(rows);
        boolean closed = windows.isEmpty();
        return SalonHoursDto.builder()
                .dayOfWeek(day)
                .closed(closed)
                .windows(windows)
                .openAt(closed ? null : windows.get(0).getOpenAt())   // rétro-compat = 1ʳᵉ plage
                .closeAt(closed ? null : windows.get(0).getCloseAt())
                .build();
    }

    /** Plages ouvertes (triées par heure d'ouverture) d'une liste de lignes. */
    private List<TimeWindowDto> toWindows(List<SalonHours> rows) {
        if (rows == null) return List.of();
        return rows.stream()
                .filter(h -> !h.isClosed() && h.getOpenAt() != null && h.getCloseAt() != null)
                .sorted(Comparator.comparing(SalonHours::getOpenAt))
                .map(h -> TimeWindowDto.builder().openAt(h.getOpenAt()).closeAt(h.getCloseAt()).build())
                .collect(Collectors.toList());
    }

    @Override
    public List<SalonHoursDto> updateWeeklyHours(Long salonId, List<SalonHoursDto> hours) {
        Salon salon = salon(salonId);
        if (hours == null) {
            throw new BusinessException("La liste des horaires est requise");
        }

        // Pour chaque jour fourni : on supprime les lignes existantes puis on réinsère une ligne par plage.
        for (SalonHoursDto dto : hours) {
            if (dto.getDayOfWeek() == null) {
                throw new BusinessException("dayOfWeek requis pour chaque ligne d'horaire");
            }
            List<TimeWindowDto> windows = normalizeWindows(dto);
            validateWindows(windows, dto.getDayOfWeek().name());

            hoursRepository.deleteBySalon_IdAndDayOfWeek(salonId, dto.getDayOfWeek());
            hoursRepository.flush();

            if (dto.isClosed() || windows.isEmpty()) {
                // Jour fermé : une ligne marqueur closed=true.
                hoursRepository.save(SalonHours.builder()
                        .salon(salon).dayOfWeek(dto.getDayOfWeek()).closed(true).build());
            } else {
                for (TimeWindowDto w : windows) {
                    hoursRepository.save(SalonHours.builder()
                            .salon(salon).dayOfWeek(dto.getDayOfWeek())
                            .closed(false).openAt(w.getOpenAt()).closeAt(w.getCloseAt()).build());
                }
            }
        }
        log.info("Horaires hebdo mis à jour pour salon {}", salonId);
        return getWeeklyHours(salonId);
    }

    /** Plages du DTO (rétro-compat : openAt/closeAt = plage unique si {@code windows} absent). */
    private List<TimeWindowDto> normalizeWindows(SalonHoursDto dto) {
        if (dto.isClosed()) return List.of();
        if (dto.getWindows() != null && !dto.getWindows().isEmpty()) {
            return dto.getWindows();
        }
        if (dto.getOpenAt() != null && dto.getCloseAt() != null) {
            return List.of(TimeWindowDto.builder().openAt(dto.getOpenAt()).closeAt(dto.getCloseAt()).build());
        }
        return List.of();
    }

    /** Valide les plages d'un jour (plage de nuit autorisée : fermeture le lendemain, ≤ 08:00). */
    private void validateWindows(List<TimeWindowDto> windows, String label) {
        com.salahtech.BarberShop_Apis.Utils.OpeningHours.validate(windows, label);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalonExceptionDto> listExceptions(Long salonId, boolean upcoming) {
        salon(salonId);
        List<SalonException> rows = upcoming
                ? exceptionRepository.findBySalon_IdAndDateGreaterThanEqualOrderByDateAsc(salonId, LocalDate.now())
                : exceptionRepository.findBySalon_IdOrderByDateAsc(salonId);
        return rows.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public SalonExceptionDto createException(Long salonId, SalonExceptionDto req) {
        Salon salon = salon(salonId);
        if (req.getDate() == null) {
            throw new BusinessException("La date de l'exception est requise");
        }
        validateRange(req.isClosed(), req.getOpenAt(), req.getCloseAt(), req.getDate().toString());

        // Upsert : une seule exception par date.
        SalonException row = exceptionRepository.findBySalon_IdAndDate(salonId, req.getDate())
                .orElseGet(() -> SalonException.builder().salon(salon).date(req.getDate()).build());
        row.setClosed(req.isClosed());
        row.setOpenAt(req.isClosed() ? null : req.getOpenAt());
        row.setCloseAt(req.isClosed() ? null : req.getCloseAt());
        row.setReason(req.getReason());
        return toDto(exceptionRepository.save(row));
    }

    @Override
    public void deleteException(Long salonId, Long exceptionId) {
        SalonException row = exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Exception introuvable: " + exceptionId));
        if (row.getSalon() == null || !row.getSalon().getId().equals(salonId)) {
            throw new BusinessException("Cette exception n'appartient pas au salon " + salonId);
        }
        exceptionRepository.delete(row);
    }

    @Override
    @Transactional(readOnly = true)
    public EffectiveHoursDto getEffectiveHours(Long salonId, LocalDate date) {
        salon(salonId);
        // 1) Exception ponctuelle prioritaire (reste mono-plage).
        SalonException exc = exceptionRepository.findBySalon_IdAndDate(salonId, date).orElse(null);
        if (exc != null) {
            List<TimeWindowDto> windows = (exc.isClosed() || exc.getOpenAt() == null || exc.getCloseAt() == null)
                    ? List.of()
                    : List.of(TimeWindowDto.builder().openAt(exc.getOpenAt()).closeAt(exc.getCloseAt()).build());
            return effective(date, windows, "EXCEPTION", exc.getReason());
        }
        // 2) Sinon, semaine type (multi-plages).
        List<TimeWindowDto> windows = toWindows(
                hoursRepository.findBySalon_IdAndDayOfWeekOrderByOpenAtAsc(salonId, date.getDayOfWeek()));
        return effective(date, windows, "WEEKLY", null);
    }

    private EffectiveHoursDto effective(LocalDate date, List<TimeWindowDto> windows, String source, String reason) {
        boolean closed = windows.isEmpty();
        return EffectiveHoursDto.builder()
                .date(date).closed(closed)
                .windows(windows)
                .openAt(closed ? null : windows.get(0).getOpenAt())                    // rétro-compat
                .closeAt(closed ? null : windows.get(windows.size() - 1).getCloseAt()) // fin de journée
                .source(source).reason(reason)
                .build();
    }

    // ----- helpers -----

    private void validateRange(boolean closed, java.time.LocalTime open, java.time.LocalTime close, String label) {
        if (closed) return;
        if (open == null || close == null) {
            throw new BusinessException("Horaires requis (ouverture/fermeture) pour " + label);
        }
        com.salahtech.BarberShop_Apis.Utils.OpeningHours.validate(
                List.of(TimeWindowDto.builder().openAt(open).closeAt(close).build()), label);
    }

    private SalonExceptionDto toDto(SalonException e) {
        return SalonExceptionDto.builder()
                .id(e.getId())
                .date(e.getDate())
                .closed(e.isClosed())
                .openAt(e.getOpenAt())
                .closeAt(e.getCloseAt())
                .reason(e.getReason())
                .build();
    }
}
