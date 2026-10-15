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
                log.info("[MANUAL_DEQUEUE_ORCHESTRATOR] Manual-only dequeue skipped because a normal SessionWorker is active | initiativeId={}",
                        initiativeId);

                handleActiveNormalWorker(activeWorker, request);
            }
            else {
                log.info("[MANUAL_DEQUEUE_ORCHESTRATOR] No active SessionWorker was found, creating a new one | initiativeId={}",
                        initiativeId);

                handleManualOnlyWorker(request);
            }
        } catch (Exception ex) {
            /*
             * No worker has reliably taken ownership of the request.
             * Release the initiative so a later manual request can proceed.
             */
            request.onCompleted().run();

            log.error("[MANUAL_DEQUEUE_ORCHESTRATOR] Manual ownership released: Unexpected orchestration failure | initiativeId={}",
                    request.initiativeId(), ex);
        }
    }

    private void handleActiveNormalWorker(NormalSessionWorker activeWorker, ManualDequeueRequest request) {
        boolean accepted = activeWorker.tryEnqueueManualRequest(request);

        if (!accepted) {
            request.onCompleted().run();

            log.info("[MANUAL_DEQUEUE_ORCHESTRATOR] Manual ownership released: Manual request could not be queued on active NormalSessionWorker | initiativeId={}",
                    request.initiativeId());

            return;
        }

        log.info("[MANUAL_DEQUEUE_ORCHESTRATOR] Manual request queued on active SessionWorker | initiativeId={}",
                request.initiativeId());
    }

    private void handleManualOnlyWorker(ManualDequeueRequest request) {
        try {
            ManualOnlySessionWorker manualOnlySessionWorker = sessionWorkerFactory.createManualOnly(
                    request.initiativeId(),
                    request.size(),
                    request.onCompleted());

            log.info("[MANUAL_DEQUEUE_ORCHESTRATOR] Manual-only SessionWorker scheduled | initiativeId={}",
                    request.initiativeId());

            /*
             * This runs on the current manualDequeueOrchestratorExecutor thread,
             * not on the normal SessionWorker pool.
             */
            manualOnlySessionWorker.run();
        } catch (Exception ex) {
            /*
             * Factory failure or run exception.
             */
            request.onCompleted().run();

            log.error("[MANUAL_DEQUEUE_ORCHESTRATOR] Manual ownership released: Manual-only SessionWorker could not be scheduled | initiativeId={}",
                    request.initiativeId(), ex);
        }
    }
}
