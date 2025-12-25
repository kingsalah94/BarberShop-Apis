package com.salahtech.BarberShop_Apis.Dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.salahtech.BarberShop_Apis.models.Availability;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvailabilityDto {

    private Long id;
    private Long barberId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date; // one-off (ponctuel)

    @JsonFormat(pattern = "EEEE") // Full name of the day
    private DayOfWeek dayOfWeek; // recurring (récurrent)

    // ⚠️ IMPORTANT : Ces champs correspondent à l'entité Availability
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startAt;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endAt;

    private Boolean isAvailable;
    private String note;

    // ========== CHAMPS POUR LES EXCEPTIONS ==========

    /**
     * Type d'exception : "CLOSED" (fermé) ou "CUSTOM" (horaires modifiés)
     * Utilisé uniquement pour les exceptions ponctuelles (date != null)
     */
    private String exceptionType;

    /**
     * Raison de l'exception
     * Exemples: "Congés annuels", "Formation", "Événement spécial"
     * Utilisé uniquement pour les exceptions ponctuelles (date != null)
     */
    private String reason;

    // ========== FIN CHAMPS EXCEPTIONS ==========

    private List<LocalTime> availableSlots; // aligne avec l'entité
    private List<LocalTime> bookedSlots;

    /**
     * Convertit une entité Availability en DTO
     * 
     * @param a L'entité Availability
     * @return Le DTO correspondant
     */
    public static AvailabilityDto fromEntity(Availability a) {
        if (a == null)
            return null;
        return AvailabilityDto.builder()
                .id(a.getId())
                .barberId(a.getBarber() != null ? a.getBarber().getId() : null)
                .date(a.getDate())
                .dayOfWeek(a.getDayOfWeek())
                .startAt(a.getStartAt()) // ✅ startTime depuis l'entité
                .endAt(a.getEndAt()) // ✅ endTime depuis l'entité
                .isAvailable(a.getIsAvailable())
                .note(a.getNote())
                .exceptionType(a.getExceptionType())
                .reason(a.getReason())
                .availableSlots(a.getAvailableSlots())
                .bookedSlots(a.getBookedSlots())
                .build();
    }

    /**
     * Convertit un DTO en entité Availability (sans lier le barber ici)
     * 
     * @param dto Le DTO
     * @return L'entité correspondante
     */
    public static Availability toEntity(AvailabilityDto dto) {
        if (dto == null)
            return null;

        Availability a = new Availability();
        a.setId(dto.getId());
        a.setDate(dto.getDate());
        a.setDayOfWeek(dto.getDayOfWeek());

        // ✅ MAPPING CORRECT : startTime/endTime vers l'entité
        a.setStartAt(dto.getStartAt());
        a.setEndAt(dto.getEndAt());

        a.setIsAvailable(dto.getIsAvailable());
        a.setNote(dto.getNote());
        a.setExceptionType(dto.getExceptionType());
        a.setReason(dto.getReason());
        a.setAvailableSlots(dto.getAvailableSlots());
        a.setBookedSlots(dto.getBookedSlots());

        return a;
    }

    /**
     * Validation du DTO avant conversion
     */
    public boolean isValid() {
        // Un créneau doit avoir soit date soit dayOfWeek
        if (date == null && dayOfWeek == null) {
            return false;
        }

        // Les créneaux récurrents doivent avoir startTime et endTime
        if (date == null && dayOfWeek != null) {
            if (startAt == null || endAt == null) {
                return false;
            }
            return startAt.isBefore(endAt);
        }

        // Les exceptions CLOSED peuvent ne pas avoir d'horaires
        if ("CLOSED".equals(exceptionType)) {
            return true;
        }

        // Sinon, vérifier les horaires si présents
        if (startAt != null && endAt != null) {
            return startAt.isBefore(endAt);
        }

        return true;
    }
}