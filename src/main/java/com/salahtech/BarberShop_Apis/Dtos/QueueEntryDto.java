package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.PaymentMethod;
import com.salahtech.BarberShop_Apis.Enums.PaymentStatus;
import com.salahtech.BarberShop_Apis.Enums.QueueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueEntryDto {
    private Long id;
    private String ticketNumber;
    private String customerName;
    private String customerPhone;
    private int numberOfPeople;
    private Long salonId;
    private String salonName;
    private Long clientId;
    private Long preferredEmployeeId;
    private Long assignedEmployeeId;
    private String assignedEmployeeName;
    private Long workstationId;
    private String workstationName;
    private LocalDateTime checkInTime;
    private LocalDateTime calledAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer estimatedWaitTime;
    private QueueStatus status;
    private String statusDisplayName;
    private Integer position;
    private String notes;
    // ----- Paiement -----
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;          // montant encaissé (si payé)
    private BigDecimal estimatedPrice;  // montant à encaisser (calculé)
}