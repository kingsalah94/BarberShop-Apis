package com.salahtech.BarberShop_Apis.Dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Réservation orientée salon (vue client + staff). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonBookingDto {
    private Long id;
    private Long salonId;
    private String salonName;
    private Long serviceId;
    private String serviceName;
    private Integer durationMinutes;
    private Long clientId;
    private String clientName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endAt;

    private String status;
    private BigDecimal totalPrice;
    private Long workstationId;
    private String workstationName;
    private Long employeeId;
    private String employeeName;
    private String notes;
    /** True si le client a déjà laissé un avis pour cette réservation. */
    private Boolean reviewed;

    // ── Passage en salon (file d'attente liée au rendez-vous) : suivi client + paiement ──
    /** Ticket de file (ex. « R-004 ») une fois le client arrivé ; null avant. */
    private String visitTicket;
    /** WAITING | NOTIFIED | IN_CHAIR | COMPLETED | CANCELLED | NO_SHOW ; null tant que le client n'est pas arrivé. */
    private String visitStatus;
    /** Position dans la file (1 = prochain) quand visitStatus = WAITING. */
    private Integer visitPosition;
    private String visitEmployeeName;
    private String visitWorkstationName;
    /** PENDING | PAID | … */
    private String paymentStatus;
    /** CASH | MOBILE_MONEY | CREDIT_CARD */
    private String paymentMethod;
    /** Montant encaissé (0 si visite offerte). */
    private java.math.BigDecimal amountPaid;
    /** Visite offerte par la fidélité. */
    private Boolean rewardUsed;
}
