package it.gov.pagopa.ranker.connector.event.consumer;

import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.ranker.service.ranker.CycleOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RankerConsumerController{

  private final InitiativeCountersService initiativeCountersService;
  private final boolean forceStopped;
  private final CycleOrchestrator orchestrator;


  public RankerConsumerController(InitiativeCountersService initiativeCountersService,
                                  @Value("${app.ranker.forceStopped:false}") boolean forceStopped,
                                  CycleOrchestrator orchestrator) {
    this.initiativeCountersService = initiativeCountersService;
    this.forceStopped = forceStopped;
    this.orchestrator = orchestrator;
  }

  /** Start “operativo”: parte solo se forceStopped=false e budget=true */
  @Scheduled(cron = "${app.initiative.schedule-check-budget}")
  public void startIfAllowed() {
    if (forceStopped) {
      log.info("[FORCE_STOPPED] Consumer start skipped (forceStopped=true)");
      return;
    }

    if (!initiativeCountersService.hasAvailableBudget()) {
      log.info("[BUDGET_CONTEXT_START] Consumer start skipped (budget=false)");
      return;
    }

    orchestrator.reconcile();
  }

}