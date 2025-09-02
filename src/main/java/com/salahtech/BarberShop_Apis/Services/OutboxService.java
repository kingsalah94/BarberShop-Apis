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
  public UUID add(String aggregateType, String aggregateId, String eventType, Map<String, Object> payload,
      Map<String, Object> headers) {
    var id = UUID.randomUUID();
    var now = OffsetDateTime.now();

    String payloadJson;
    String headersJson = null;
    try {
      payloadJson = mapper.writeValueAsString(payload);
      if (headers != null)
        headersJson = mapper.writeValueAsString(headers);
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

  @Transactional
  public UUID enqueueEmail(String to, String subject, String html) {
    var payload = Map.of("to", to, "subject", subject, "html", html);
    var event = new OutboxEvent();
    event.setId(UUID.randomUUID());
    event.setAggregateType("EMAIL");
    event.setAggregateId(to);
    event.setEventType("SEND_EMAIL");
    event.setPayload(writeJson(payload));
    event.setStatus(OutboxStatus.PENDING);
    event.setAttempts(0);
    event.setCreatedAt(OffsetDateTime.now());
    event.setUpdatedAt(OffsetDateTime.now());
    event.setNextAttemptAt(OffsetDateTime.now());
    repo.save(event);
    return event.getId();
  }

  private String writeJson(Object o) {
    try {
      return mapper.writeValueAsString(o);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
