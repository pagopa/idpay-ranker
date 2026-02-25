package it.gov.pagopa.ranker.connector.event.consumer;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RankerConsumerController {

  private final InitiativeCountersService initiativeCountersService;
  private final ServiceBusProcessorClientFactory factory;
  private final RankerMessageHandler handler;
  private final boolean forceStopped;

  private ServiceBusProcessorClient processorClient;

  public RankerConsumerController(
      InitiativeCountersService initiativeCountersService,
      ServiceBusProcessorClientFactory factory,
      RankerMessageHandler handler,
      @Value("${app.ranker.forceStopped:false}") boolean forceStopped
  ) {
    this.initiativeCountersService = initiativeCountersService;
    this.factory = factory;
    this.handler = handler;
    this.forceStopped = forceStopped;
  }

  @PostConstruct
  public void init() {
    this.processorClient = factory.create(handler);
    log.info("[FORCE_STOPPED] Initiative processing is force stopped: {}", forceStopped);
  }

  public boolean isConsumerRunning() {
    return processorClient != null && processorClient.isRunning();
  }

  /** Start “operativo”: parte solo se forceStopped=false e budget=true */
  public void startIfAllowed() {
    if (processorClient == null) {
      log.info("[RANKER_CONTEXT] startIfAllowed skipped (processorClient null)");
      return;
    }
    if (forceStopped) {
      log.info("[FORCE_STOPPED] Consumer start skipped (forceStopped=true)");
      return;
    }
    boolean hasBudget = initiativeCountersService.hasAvailableBudget();
    if (!hasBudget) {
      log.info("[BUDGET_CONTEXT_START] Consumer start skipped (budget=false)");
      return;
    }
    if (!processorClient.isRunning()) {
      processorClient.start();
      log.info("[RANKER_CONTEXT] Consumer started");
    }
  }

  /** Stop “operativo” (budget exhausted). */
  public void stopForBudgetExhausted() {
    stopInternal("budget exhausted", false);
  }

  /** Stop su shutdown applicazione. */
  public void stopOnShutdown() {
    stopInternal("shutdown", true);
  }

  private void stopInternal(String reason, boolean close) {
    if (processorClient != null && processorClient.isRunning()) {
      if (close) {
        processorClient.close();
        log.info("[RANKER_CONTEXT] Consumer closed ({})", reason);
      } else {
        processorClient.stop();
        log.info("[RANKER_CONTEXT] Consumer stopped ({})", reason);
      }
    }
  }

  @Scheduled(cron = "${app.initiative.schedule-check-budget}")
  public void checkResidualBudgetAndStartConsumer() {
    startIfAllowed();
  }

  @SuppressWarnings("unused") // invoked by Spring via @EventListener
  @EventListener(BudgetExhaustedEvent.class)
  public void onBudgetExhausted() {
    stopForBudgetExhausted();
  }
}