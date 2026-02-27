package it.gov.pagopa.ranker.connector.event.consumer;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import it.gov.pagopa.ranker.exception.BudgetExhaustedException;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RankerMessageHandler {

  private final RankerService rankerService;
  private final ApplicationEventPublisher publisher;

  public RankerMessageHandler(RankerService rankerService, ApplicationEventPublisher publisher) {
    this.rankerService = rankerService;
    this.publisher = publisher;
  }

  public void handle(ServiceBusReceivedMessageContext context) {
    try {
      rankerService.execute(context.getMessage());
    } catch (BudgetExhaustedException e) {
      log.error("[BUDGET_CONTEXT] Budget exhausted.");
      publisher.publishEvent(BudgetExhaustedEvent.of("initiative budget exhausted"));
    } catch (Exception e) {
      log.error("[RANKER_CONTEXT] Error processing message", e);
    }
  }
}