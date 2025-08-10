package com.salahtech.BarberShop_Apis.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salahtech.BarberShop_Apis.outbox.OutboxEvent;
import com.salahtech.BarberShop_Apis.outbox.OutboxStatus;
import com.salahtech.BarberShop_Apis.reppsitories.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

  private final OutboxEventRepository repo;
  private final ObjectMapper mapper;

  @Transactional
  public UUID add(String aggregateType, String aggregateId, String eventType, Map<String, Object> payload, Map<String, Object> headers) {
    var id = UUID.randomUUID();
    var now = OffsetDateTime.now();

    String payloadJson;
    String headersJson = null;
    try {
      payloadJson = mapper.writeValueAsString(payload);
      if (headers != null) headersJson = mapper.writeValueAsString(headers);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot serialize outbox payload", e);
    }

    var event = OutboxEvent.builder()
        .id(id)
        .aggregateType(aggregateType)
        .aggregateId(aggregateId)
        .eventType(eventType)
        .payload(payloadJson)
        .headers(headersJson)
        .status(OutboxStatus.PENDING)
        .attempts(0)
        .nextAttemptAt(now)
        .createdAt(now)
        .updatedAt(now)
        .build();

    repo.save(event);
    return id;
  }
}

