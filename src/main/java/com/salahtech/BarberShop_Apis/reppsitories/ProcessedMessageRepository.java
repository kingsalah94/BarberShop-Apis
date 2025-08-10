package com.salahtech.BarberShop_Apis.reppsitories;


import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.salahtech.BarberShop_Apis.notifications.ProcessedMessage;

import java.util.UUID;

public interface ProcessedMessageRepository extends CrudRepository<ProcessedMessage, UUID> {

  @Modifying
  @Query(value = "INSERT INTO processed_messages(message_id) VALUES (?1)", nativeQuery = true)
  void markProcessed(UUID messageId);
}

