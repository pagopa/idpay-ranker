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
        String initiativeId = request.initiativeId();
        /*
         * Manual dequeue workflow:
         * find SessionWorker, take over cron work safely,
         * execute the manual dequeue, then release control.
         */
        NormalSessionWorker activeWorker = cycleHandlerState
                .getActiveSessions().get(initiativeId);

        if (activeWorker != null) {
            log.info("Manual-only dequeue skipped because a normal SessionWorker is active | initiativeId={}",
                    initiativeId);

            handleActiveNormalWorker(activeWorker, request);
        }
        else {
            log.info("No active SessionWorker was found, creating a new one | initiativeId={}",
                    initiativeId);

            handleManualOnlyWorker(request);
        }
    }

    private void handleActiveNormalWorker(NormalSessionWorker activeWorker, ManualDequeueRequest request) {
        boolean accepted = activeWorker.tryEnqueueManualRequest(request);

        if (!accepted) {
            request.onCompleted().run();

            log.info("[MANUAL_DEQUEUE] Manual request could not be queued on active NormalSessionWorker | initiativeId={}",
                    request.initiativeId());

            return;
        }

        log.info("[MANUAL_DEQUEUE] Manual request queued on active SessionWorker | initiativeId={}",
                request.initiativeId());
    }

    private void handleManualOnlyWorker(ManualDequeueRequest request) {
        try {
            ManualOnlySessionWorker manualOnlySessionWorker = sessionWorkerFactory.createManualOnly(
                    request.initiativeId(),
                    request.size(),
                    request.onCompleted());

            /*
             * This runs on a thread from manualDequeueOrchestratorExecutor,
             * not on the normal SessionWorker pool.
             */
            manualOnlySessionWorker.run();

            log.info("[MANUAL_DEQUEUE] Manual-only SessionWorker scheduled | initiativeId={}",
                    request.initiativeId());

        } catch (Exception exception) {
            /*
             * Factory failure or executor rejection:
             * the worker never took ownership of the request.
             */
            request.onCompleted().run();

            log.error("[MANUAL_DEQUEUE] Manual-only SessionWorker could not be scheduled | initiativeId={}",
                    request.initiativeId(),
                    exception);
        }
    }
}
