package com.salahtech.BarberShop_Apis.outbox;


import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class OutboxEvent {

  @Id
  private UUID id;

  @Column(name="aggregate_type", nullable=false)
  private String aggregateType;

  @Column(name="aggregate_id", nullable=false)
  private String aggregateId;

  @Column(name="event_type", nullable=false)
  private String eventType;

  //@Type(org.hibernate.type.JsonType.class) // si tu utilises hibernate-types, sinon @Lob String
  @Column(columnDefinition = "jsonb", nullable=false)
  private String payload;

  //@Type(org.hibernate.type.JsonType.class)
  @Column(columnDefinition = "jsonb")
  private String headers;

  @Enumerated(EnumType.STRING)
  @Column(nullable=false)
  private OutboxStatus status;

  @Column(nullable=false)
  private int attempts;

  @Column(name="next_attempt_at", nullable=false)
  private OffsetDateTime nextAttemptAt;

  @Column(name="created_at", nullable=false)
  private OffsetDateTime createdAt;

  @Column(name="updated_at", nullable=false)
  private OffsetDateTime updatedAt;
}

