package com.salahtech.BarberShop_Apis.outbox;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.salahtech.BarberShop_Apis.reppsitories.OutboxEventRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxKafkaRelay {

  private final OutboxEventRepository repo;
  private final KafkaTemplate<String, Object> kafka;
  private final OutboxRelayProps props;

  @Scheduled(fixedDelayString = "PT2S") // toutes les 2s
  public void run() {
    List<OutboxEvent> batch = repo.lockBatchForSend(OffsetDateTime.now(), props.batchSize());
    if (batch.isEmpty()) return;

    for (var event : batch) {
      try {
        // clé = id -> ordering et idempotence côté consumer
        var future = kafka.send(props.topic(), event.getId().toString(), new OutboxPayload(event));
        RecordMetadata meta = future.get(30, TimeUnit.SECONDS).getRecordMetadata();

        // Succès
        event.setStatus(OutboxStatus.SENT);
        event.setUpdatedAt(OffsetDateTime.now());
        repo.save(event);
        log.info("Outbox {} sent to {}-{}@{}", event.getId(), meta.topic(), meta.partition(), meta.offset());

      } catch (Exception ex) {
        // Échec -> backoff exponentiel
        int attempts = event.getAttempts() + 1;
        long delay = computeBackoffMs(attempts, props.backoffInitialMs(), props.backoffMultiplier(), props.backoffMaxMs());
        event.setAttempts(attempts);
        event.setStatus(attempts >= 10 ? OutboxStatus.FAILED : OutboxStatus.PENDING);
        event.setNextAttemptAt(OffsetDateTime.now().plusSeconds(delay / 1000));
        event.setUpdatedAt(OffsetDateTime.now());
        repo.save(event);
        log.warn("Outbox {} failed (attempt {}), retry in {} ms", event.getId(), attempts, delay, ex);
      }
    }
  }

  private long computeBackoffMs(int attempts, long initialMs, double mult, long maxMs) {
    double v = initialMs * Math.pow(mult, Math.max(0, attempts - 1));
    return Math.min((long) v, maxMs);
  }

  public record OutboxPayload(OutboxEvent e) {
    public String id() { return e.getId().toString(); }
    public String aggregateType() { return e.getAggregateType(); }
    public String aggregateId() { return e.getAggregateId(); }
    public String eventType() { return e.getEventType(); }
    public String payload() { return e.getPayload(); } // JSON string
    public String headers() { return e.getHeaders(); } // JSON string
    public String createdAt() { return e.getCreatedAt().toString(); }
  }
}
