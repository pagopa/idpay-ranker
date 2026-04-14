package it.gov.pagopa.ranker.connector.event.consumer;

import java.time.Clock;
import java.time.Instant;

public record BudgetExhaustedEvent(
    Instant occurredAt,
    String reason
) {
  public static BudgetExhaustedEvent of(String reason, Clock clock) {
    return new BudgetExhaustedEvent(Instant.now(clock), reason);
  }
}