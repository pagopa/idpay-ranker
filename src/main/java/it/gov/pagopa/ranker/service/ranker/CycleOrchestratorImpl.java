package it.gov.pagopa.ranker.service.ranker;

import it.gov.pagopa.ranker.config.RankerProcessorProperties;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.ranker.service.ranker.sessionworker.NormalSessionWorker;
import it.gov.pagopa.ranker.service.ranker.sessionworker.SessionWorkerFactory;
import it.gov.pagopa.ranker.service.ranker.sessionworker.registry.SessionWorkerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Service
public class CycleOrchestratorImpl implements CycleOrchestrator{
    private static final String INITIATIVES_ONBOARDING_HANDLER_FLOW = "INITIATIVES_ONBOARDING_HANDLER";

    private final CycleHandlerState cycleHandlerState;
    private final Executor executor;
    private final RankerProcessorProperties properties;
    private final SessionWorkerFactory sessionWorkerFactory;
    private final InitiativeCountersService initiativeCountersService;
    private final SessionWorkerRegistry sessionWorkerRegistry;

    public CycleOrchestratorImpl(CycleHandlerState cycleHandlerState,
                                 @Qualifier("sessionExecutor") Executor executor,
                                 RankerProcessorProperties properties,
                                 SessionWorkerFactory sessionWorkerFactory,
                                 InitiativeCountersService initiativeCountersService,
                                 SessionWorkerRegistry sessionWorkerRegistry) {
        this.cycleHandlerState = cycleHandlerState;
        this.executor = executor;
        this.properties = properties;
        this.sessionWorkerFactory = sessionWorkerFactory;
        this.initiativeCountersService = initiativeCountersService;
        this.sessionWorkerRegistry =  sessionWorkerRegistry;
    }

    @Override
    public synchronized void reconcile() {
        List<String> initiativesAvailableBudget = initiativeCountersService.retrieveInitiativesAvailableBudget();

        if (initiativesAvailableBudget.isEmpty()) {
            log.info("[{}] No initiatives with available budget to process.", INITIATIVES_ONBOARDING_HANDLER_FLOW);
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

            // Generate UUID to be used to try claim session from the common registry.
            UUID claimId = UUID.randomUUID();
            // Create NormalSessionWorker.
            NormalSessionWorker worker = sessionWorkerFactory.create(initiativeId, () -> onWorkerClose(initiativeId, claimId));
            // Try claiming the session for itself.
            boolean claimed = sessionWorkerRegistry.tryClaimNormal(initiativeId, claimId, worker);

            // Skip scheduling if session hasn't been claimed.
            if (!claimed) {
                log.info("[{}] Initiative {} is already owned by another session worker in the registry. Skipping scheduling for worker with claimId={}.",
                        INITIATIVES_ONBOARDING_HANDLER_FLOW, initiativeId, claimId);
            } else {
                cycleHandlerState.getActiveSessions().put(initiativeId, worker);
                boolean workerExecuted = tryExecutingWorker(initiativeId, worker, claimId);
                if (!workerExecuted) {
                    return;
                }
            }
        }
    }

    private synchronized boolean tryExecutingWorker(String initiativeId, NormalSessionWorker worker, UUID claimId) {
        try {
            executor.execute(worker);
        } catch (RejectedExecutionException ex) {

            boolean released = sessionWorkerRegistry.release(initiativeId, claimId);

            cycleHandlerState.getActiveSessions().remove(initiativeId, worker);
            cycleHandlerState.getPendingSessions().addFirst(initiativeId);

            if (!released) {
                log.error("[{}] Could not release session-worker claim for initiative {} and claimId={}.",
                        INITIATIVES_ONBOARDING_HANDLER_FLOW,
                        initiativeId,
                        claimId
                );
            }

            log.error("[{}] Worker execution was rejected for initiative {} and claimId={}. The initiative was returned to the pending queue.",
                    INITIATIVES_ONBOARDING_HANDLER_FLOW, initiativeId, claimId, ex);

            return false;
        }

        log.info("[{}] Scheduled worker for initiative {}. Active={} Pending={}",
                INITIATIVES_ONBOARDING_HANDLER_FLOW,
                initiativeId,
                cycleHandlerState.getActiveSessions().keySet(),
                cycleHandlerState.getPendingSessions());

        return true;
    }

    private synchronized void onWorkerClose(String sessionId, UUID claimId) {
        try {
            boolean released = sessionWorkerRegistry.release(sessionId, claimId);

            if (!released) {
                log.warn("[{}] Could not release session-worker claim for initiative {} and claimId={}.",
                        INITIATIVES_ONBOARDING_HANDLER_FLOW,
                        sessionId,
                        claimId
                );
            }
        } catch (Exception ex) {
            log.error("[{}] Unexpected error while releasing session-worker claim for initiative {} and claimId={}",
                    INITIATIVES_ONBOARDING_HANDLER_FLOW,
                    sessionId,
                    claimId,
                    ex
            );
        } finally {
            cycleHandlerState.getActiveSessions().remove(sessionId);
        }

        log.info("[{}] Worker with claimId={} completed for initiative {}. Active={} Pending={}",
                INITIATIVES_ONBOARDING_HANDLER_FLOW,
                claimId,
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
