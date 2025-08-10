package com.salahtech.BarberShop_Apis.notifications;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "processed_messages")
@Data
public class ProcessedMessage {
  @Id
  private UUID messageId;
}

