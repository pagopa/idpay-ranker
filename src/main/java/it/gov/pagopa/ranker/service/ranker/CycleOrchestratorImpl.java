package it.gov.pagopa.ranker.service.ranker;

import it.gov.pagopa.ranker.config.RankerProcessorProperties;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class CycleOrchestratorImpl implements CycleOrchestrator{
    private static final String INITIATIVES_ONBOARDING_HANDLER_FLOW = "INITIATIVES_ONBOARDING_HANDLER";

    private final CycleHandlerState cycleHandlerState;
    private final Executor executor;
    private final RankerProcessorProperties properties;
    private final SessionWorkerFactory sessionWorkerFactory;
    private final InitiativeCountersService initiativeCountersService;

    public CycleOrchestratorImpl(CycleHandlerState cycleHandlerState,
                                 @Qualifier("sessionExecutor") Executor executor,
                                 RankerProcessorProperties properties, SessionWorkerFactory sessionWorkerFactory, InitiativeCountersService initiativeCountersService) {
        this.cycleHandlerState = cycleHandlerState;
        this.executor = executor;
        this.properties = properties;
        this.sessionWorkerFactory = sessionWorkerFactory;
        this.initiativeCountersService = initiativeCountersService;
    }

    @Override
    public synchronized void reconcile() {
        List<String> initiativesAvailableBudget = initiativeCountersService.retrieveInitiativesAvailableBudget();

        if (initiativesAvailableBudget.isEmpty()) {
            log.info("[{}}] No initiatives with available budget to process.", INITIATIVES_ONBOARDING_HANDLER_FLOW);
            return;
        }

        if (!cycleHandlerState.isCycleProcessing()) {

            if (cycleHandlerState.openCycle()) {
                cycleHandlerState.getPendingSessions().addAll(initiativesAvailableBudget);
                log.info("[{}] Cycle opened. Pending initiatives: {}", INITIATIVES_ONBOARDING_HANDLER_FLOW, cycleHandlerState.getPendingSessions());
            }
        } else {
            List<String> newInitiativesAvailable = initiativesAvailableBudget.stream()
                    .filter(initiative -> !cycleHandlerState.isInActive(initiative) && !cycleHandlerState.isInPending(initiative)).toList();
            cycleHandlerState.getPendingSessions().addAll(newInitiativesAvailable);
            log.info("[{}] Cycle already active. Pending initiatives updated: {}", INITIATIVES_ONBOARDING_HANDLER_FLOW, cycleHandlerState.getPendingSessions());
        }

        fillAvailableSlots();

        if (cycleHandlerState.isCompleted()) {
            cycleHandlerState.closeCycle();
            log.info("[{}] Cycle completed. No active or pending initiatives remaining.", INITIATIVES_ONBOARDING_HANDLER_FLOW);
        }
    }

    private synchronized void fillAvailableSlots() {
        while (cycleHandlerState.hasFreeSlot(properties.getMaxParallelSessions())
                && !cycleHandlerState.getPendingSessions().isEmpty()) {

            String initiativeId = cycleHandlerState.getPendingSessions().pollFirst();
            if (initiativeId == null) {
                log.info("[{}] No pending initiatives available for scheduling.", INITIATIVES_ONBOARDING_HANDLER_FLOW);
                return;
            }

            if (cycleHandlerState.getActiveSessions().containsKey(initiativeId)) {
                log.debug("[{}] Initiative {} is already active. Skipping scheduling.", INITIATIVES_ONBOARDING_HANDLER_FLOW, initiativeId);
                continue;
            }

            SessionWorker worker = sessionWorkerFactory.create(initiativeId, () -> onWorkerClose(initiativeId));
            cycleHandlerState.getActiveSessions().put(initiativeId, worker);
            executor.execute(worker);

            log.info("[{}] Scheduled worker for initiative {}. Active={} Pending={}",
                    INITIATIVES_ONBOARDING_HANDLER_FLOW,
                    initiativeId,
                    cycleHandlerState.getActiveSessions().keySet(),
                    cycleHandlerState.getPendingSessions());
        }
    }

    private synchronized void onWorkerClose(String sessionId) {
        cycleHandlerState.getActiveSessions().remove(sessionId);

        log.info("[{}] Worker completed for initiative {}. Active={} Pending={}",
                INITIATIVES_ONBOARDING_HANDLER_FLOW,
                sessionId,
                cycleHandlerState.getActiveSessions().keySet(),
                cycleHandlerState.getPendingSessions());

        if (cycleHandlerState.isCompleted()) {
            cycleHandlerState.closeCycle();
            log.info("[{}] Cycle closed after completion of the last active worker.", INITIATIVES_ONBOARDING_HANDLER_FLOW);
            return;
        }

        fillAvailableSlots();
    }
}
