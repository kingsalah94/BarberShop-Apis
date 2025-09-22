package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_ts", columnList = "ts"),
        @Index(name = "idx_audit_user", columnList = "userId"),
        @Index(name = "idx_audit_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private OffsetDateTime ts;
    private Long userId;

    @Column(length = 64)
    private String action; // ex: "SECURITY_SETTINGS_UPDATE", "ROLE_CREATE"
    @Column(length = 64)
    private String subjectType; // ex: "ROLE", "SERVICE", "USER"
    private String subjectId; // ex: "42"

    @Column(length = 64)
    private String ip;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    @PrePersist
    void onCreate() {
        ts = (ts == null ? OffsetDateTime.now() : ts);
    }
}
