package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.outbox.OutboxEvent;
import com.salahtech.BarberShop_Apis.outbox.OutboxStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OutboxService {

  /**
   * Méthode générique de bas niveau pour ajouter n'importe quel événement
   */
  UUID add(String aggregateType, String aggregateId, String eventType, Map<String, Object> payload,
      Map<String, Object> headers);

  /**
   * Cas métier : Mettre en file d'attente un email
   */
  UUID enqueueEmail(String to, String subject, String html);

  List<OutboxEvent> getEventsByStatus(OutboxStatus status);

  /** Relance manuellement un event spécifique via son ID */
  void retryEvent(UUID id);

  /** Relance tous les events qui sont en échec (FAILED) */
  int retryAllFailed();
}