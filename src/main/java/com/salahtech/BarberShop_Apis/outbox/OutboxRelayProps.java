package com.salahtech.BarberShop_Apis.outbox;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.outbox")
public class OutboxRelayProps {
  private String topic;
  private int batchSize = 100;
  private Backoff backoff = new Backoff();

  public static class Backoff {
    private long initialMs = 2000;
    private double multiplier = 2.0;
    private long maxMs = 60000;
    // getters/setters
  }
  // getters/setters + flatten
  public String topic() { return topic; }
  public int batchSize() { return batchSize; }
  public long backoffInitialMs() { return backoff.initialMs; }
  public double backoffMultiplier() { return backoff.multiplier; }
  public long backoffMaxMs() { return backoff.maxMs; }
  // setters ...
}
