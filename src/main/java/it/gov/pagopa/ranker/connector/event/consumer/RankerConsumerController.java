package it.gov.pagopa.ranker.connector.event.consumer;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RankerConsumerController implements SmartLifecycle {

  private final InitiativeCountersService initiativeCountersService;
  private final ServiceBusProcessorClientFactory factory;
  private final RankerMessageHandler handler;
  private final boolean forceStopped;
  private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

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

  @Override
  public boolean isRunning() {
    return processorClient != null && processorClient.isRunning();
  }

  /** Start “operativo”: parte solo se forceStopped=false e budget=true */
  @Scheduled(cron = "${app.initiative.schedule-check-budget}")
  public void startIfAllowed() {
    if (shuttingDown.get()) {
      return;
    }
    if (processorClient == null) {
      log.info("[RANKER_CONTEXT] startIfAllowed skipped (processorClient null)");
      return;
    }
    if (forceStopped) {
      log.info("[FORCE_STOPPED] Consumer start skipped (forceStopped=true)");
      return;
    }
    if (!initiativeCountersService.hasAvailableBudget()) {
      log.info("[BUDGET_CONTEXT_START] Consumer start skipped (budget=false)");
      return;
    }
    if (!processorClient.isRunning()) {
      processorClient.start();
      log.info("[RANKER_CONTEXT] Consumer started");
    }
  }

  @Override
  public void start() {
    startIfAllowed();
  }

  /** Stop su shutdown applicazione. */
  @Override
  public void stop() {
    shuttingDown.set(true);
    if (processorClient != null) {
      processorClient.close();
      log.info("[RANKER_CONTEXT] Consumer closed shutdown");
    }
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE;
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  /** Stop “operativo” (budget exhausted). */
  @SuppressWarnings("unused") // invoked by Spring via @EventListener
  @EventListener(BudgetExhaustedEvent.class)
  public void onBudgetExhausted() {
    if (processorClient != null && processorClient.isRunning()) {
      processorClient.stop();
      log.info("[RANKER_CONTEXT] Consumer stopped budget exhausted");
    }
  }
}