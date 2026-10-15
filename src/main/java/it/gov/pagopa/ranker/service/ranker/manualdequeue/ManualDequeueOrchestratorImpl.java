package it.gov.pagopa.ranker.service.ranker.manualdequeue;

import it.gov.pagopa.ranker.service.ranker.CycleHandlerState;
import it.gov.pagopa.ranker.service.ranker.SessionWorkerFactory;
import it.gov.pagopa.ranker.service.ranker.sessionworker.ManualOnlySessionWorker;
import it.gov.pagopa.ranker.service.ranker.sessionworker.NormalSessionWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ManualDequeueOrchestratorImpl implements ManualDequeueOrchestrator {

    private final CycleHandlerState cycleHandlerState;
    private final SessionWorkerFactory sessionWorkerFactory;

    public ManualDequeueOrchestratorImpl(CycleHandlerState cycleHandlerState,
                                         SessionWorkerFactory sessionWorkerFactory) {
        this.cycleHandlerState = cycleHandlerState;
        this.sessionWorkerFactory = sessionWorkerFactory;
    }

    @Override
    public void orchestrate(ManualDequeueRequest request) {
        try {
            String initiativeId = request.initiativeId();
            /*
             * Manual dequeue workflow:
             * find SessionWorker, take over cron work safely,
             * execute the manual dequeue, then release control.
             */
            NormalSessionWorker activeWorker = cycleHandlerState
                    .getActiveSessions().get(initiativeId);

            if (activeWorker != null) {
                log.info("[MANUAL_DEQUEUE_ORCHESTRATOR] An active NormalSessionWorker was found, trying to enqueue a manual request | initiativeId={}",
                        initiativeId);

                handleActiveNormalWorker(activeWorker, request);
            }
            else {
                log.info("[MANUAL_DEQUEUE_ORCHESTRATOR] No active NormalSessionWorker was found, running a new one | initiativeId={}",
                        initiativeId);

                handleManualOnlyWorker(request);
            }
        } catch (Exception ex) {
            log.error("[MANUAL_DEQUEUE_ORCHESTRATOR] Unexpected orchestration failure | initiativeId={}",
                    request.initiativeId(), ex);
        }
    }

    private void handleActiveNormalWorker(NormalSessionWorker activeWorker, ManualDequeueRequest request) {
        boolean accepted = activeWorker.tryEnqueueManualRequest(request);

        if (!accepted) {
            log.info("[MANUAL_DEQUEUE_ORCHESTRATOR] Manual request could not be queued on active NormalSessionWorker | initiativeId={}",
                    request.initiativeId());

            return;
        }

        log.info("[MANUAL_DEQUEUE_ORCHESTRATOR] Manual request queued on active NormalSessionWorker | initiativeId={}",
                request.initiativeId());
    }

    private void handleManualOnlyWorker(ManualDequeueRequest request) {
        try {
            ManualOnlySessionWorker manualOnlySessionWorker = sessionWorkerFactory.createManualOnly(
                    request.initiativeId(),
                    request.size());

            log.info("[MANUAL_DEQUEUE_ORCHESTRATOR] Starting ManualOnlySessionWorker | initiativeId={} requested={}",
                    request.initiativeId(), request.size());

            /*
             * This runs on the current manualDequeueOrchestratorExecutor thread,
             * not on the normal SessionWorker pool.
             */
            manualOnlySessionWorker.run();
        } catch (Exception ex) {
            log.error("[MANUAL_DEQUEUE_ORCHESTRATOR] ManualOnlySessionWorker could not be scheduled | initiativeId={}",
                    request.initiativeId(), ex);
        }
    }
}
