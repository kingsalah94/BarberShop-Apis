package com.salahtech.BarberShop_Apis.Dtos;

import lombok.*;
import java.time.OffsetDateTime;

import com.salahtech.BarberShop_Apis.models.AuditEvent;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditEventDto {
    private Long id;
    private OffsetDateTime ts;
    private Long userId;
    private String action;
    private String subjectType;
    private String subjectId;
    private String ip;
    private String metadataJson;

    public static AuditEventDto from(AuditEvent e) {
        if (e == null)
            return null;
        return AuditEventDto.builder()
                .id(e.getId()).ts(e.getTs()).userId(e.getUserId())
                .action(e.getAction()).subjectType(e.getSubjectType())
                .subjectId(e.getSubjectId()).ip(e.getIp())
                .metadataJson(e.getMetadataJson())
                .build();
    }
}
