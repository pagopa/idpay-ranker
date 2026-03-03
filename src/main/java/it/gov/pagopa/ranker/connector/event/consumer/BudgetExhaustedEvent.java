package it.gov.pagopa.ranker.connector.event.consumer;

import java.time.Instant;

public record BudgetExhaustedEvent(
    Instant occurredAt,
    String reason
) {
  public static BudgetExhaustedEvent of(String reason) {
    return new BudgetExhaustedEvent(Instant.now(), reason);
  }
}