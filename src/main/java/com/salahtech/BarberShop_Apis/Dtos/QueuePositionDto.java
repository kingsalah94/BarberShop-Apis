package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.QueueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueuePositionDto {
    private Long queueEntryId;
    private String ticketNumber;
    private QueueStatus status;
    private int position;
    private int peopleAhead;
    private int estimatedWaitMinutes;
    /** Poste affecté (quand le client est appelé / en service). */
    private String workstationName;
}