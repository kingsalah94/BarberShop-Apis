package com.salahtech.BarberShop_Apis.reppsitories;


import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.salahtech.BarberShop_Apis.outbox.OutboxEvent;
import com.salahtech.BarberShop_Apis.outbox.OutboxStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

        List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
 

        @Query(value = """
        SELECT * FROM outbox_event
        WHERE status = 'PENDING' AND next_attempt_at <= :now
        ORDER BY created_at ASC
        """, nativeQuery = true)
        List<OutboxEvent> findReadyForSend(@Param("now") OffsetDateTime now);

      @Query(value = """
      SELECT * FROM outbox_event
      WHERE status = 'PENDING' AND next_attempt_at <= :now
      ORDER BY created_at ASC
      LIMIT :batchSize
      FOR UPDATE SKIP LOCKED
      """, nativeQuery = true)
      List<OutboxEvent> lockBatchForSend(@Param("now") OffsetDateTime now,
                                     @Param("batchSize") int batchSize);
}

