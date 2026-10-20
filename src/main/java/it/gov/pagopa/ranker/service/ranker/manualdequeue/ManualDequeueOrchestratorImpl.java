package it.gov.pagopa.ranker.service.ranker.manualdequeue;

import it.gov.pagopa.ranker.service.ranker.sessionworker.SessionWorkerFactory;
import it.gov.pagopa.ranker.service.ranker.sessionworker.ManualOnlySessionWorker;
import it.gov.pagopa.ranker.service.ranker.sessionworker.NormalSessionWorker;
import it.gov.pagopa.ranker.service.ranker.sessionworker.registry.ManualClaimResult;
import it.gov.pagopa.ranker.service.ranker.sessionworker.registry.SessionWorkerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static it.gov.pagopa.utils.CommonUtils.sanitizeString;


@Slf4j
@Service
public class ManualDequeueOrchestratorImpl implements ManualDequeueOrchestrator {

    private final SessionWorkerFactory sessionWorkerFactory;
    private final SessionWorkerRegistry sessionWorkerRegistry;

    public ManualDequeueOrchestratorImpl(SessionWorkerFactory sessionWorkerFactory,
                                         SessionWorkerRegistry sessionWorkerRegistry) {
        this.sessionWorkerFactory = sessionWorkerFactory;
        this.sessionWorkerRegistry = sessionWorkerRegistry;
    }

    @Override
    public void orchestrate(ManualDequeueRequest request) {
        try {
            String initiativeId = request.initiativeId();
            UUID claimId = UUID.randomUUID();

            // Try manually claiming the session.
            ManualClaimResult claimResult = sessionWorkerRegistry.tryClaimManual(initiativeId, claimId);

            // Evaluate the claim attempt outcome.
            switch (claimResult.outcome()) {
                case CLAIMED -> handleManualOnlyClaim(
                        request,
                        claimId
                );

                case OWNED_BY_NORMAL -> handleActiveNormalWorker(
                        claimResult.normalWorker(),
                        request
                );

                case OWNED_BY_MANUAL -> log.info(
                        "[MANUAL_DEQUEUE_ORCHESTRATOR] A ManualOnlySessionWorker already owns the initiative. "
                                + "Manual request cannot be processed now | initiativeId={}",
                        sanitizeString(initiativeId)
                );

                default -> log.error(
                        "[MANUAL_DEQUEUE_ORCHESTRATOR] Unsupported manual-claim outcome {} | initiativeId={}",
                        claimResult.outcome(),
                        sanitizeString(initiativeId)
                );
            }
        } catch (Exception ex) {
            log.error("[MANUAL_DEQUEUE_ORCHESTRATOR] Unexpected orchestration failure | initiativeId={}",
                    sanitizeString(request.initiativeId()), ex);
        }
    }

    private void handleActiveNormalWorker(NormalSessionWorker activeWorker, ManualDequeueRequest request) {
        if (activeWorker == null) {
            log.error(
                    "[MANUAL_DEQUEUE_ORCHESTRATOR] Registry returned OWNED_BY_NORMAL without a NormalSessionWorker | initiativeId={}",
                    sanitizeString(request.initiativeId())
            );
            return;
        }

        log.info("[MANUAL_DEQUEUE_ORCHESTRATOR] An active NormalSessionWorker was found, trying to enqueue a manual request | initiativeId={}",
                sanitizeString(request.initiativeId()));
        boolean accepted = activeWorker.tryEnqueueManualRequest(request);

        if (!accepted) {
            log.info("[MANUAL_DEQUEUE_ORCHESTRATOR] Manual request could not be queued on active NormalSessionWorker | initiativeId={}",
                    sanitizeString(request.initiativeId()));

            return;
        }

        log.info("[MANUAL_DEQUEUE_ORCHESTRATOR] Manual request queued on active NormalSessionWorker | initiativeId={}",
                sanitizeString(request.initiativeId()));
    }

    private void handleManualOnlyClaim(ManualDequeueRequest request, UUID claimId) {
        try {
            log.info("[MANUAL_DEQUEUE_ORCHESTRATOR] No active NormalSessionWorker was found, running a new manual only one | initiativeId={} requested={}",
                    sanitizeString(request.initiativeId()), request.size());

            ManualOnlySessionWorker manualOnlySessionWorker = sessionWorkerFactory.createManualOnly(
                    request.initiativeId(),
                    request.size());

            /*
             * This runs on the current manualDequeueOrchestratorExecutor thread,
             * not on the normal SessionWorker pool.
             */
            manualOnlySessionWorker.run();
        } catch (Exception ex) {
            log.error("[MANUAL_DEQUEUE_ORCHESTRATOR] ManualOnlySessionWorker could not be scheduled | initiativeId={}",
                    sanitizeString(request.initiativeId()), ex);
        } finally {
            releaseManualClaim(request.initiativeId(), claimId);
        }
    }

    private void releaseManualClaim(
            String initiativeId,
            UUID claimId
    ) {
        try {
            boolean released = sessionWorkerRegistry.release(initiativeId, claimId);

            if (!released) {
                log.warn("[MANUAL_DEQUEUE_ORCHESTRATOR] Could not release manual session-worker claim | initiativeId={} claimId={}",
                        sanitizeString(initiativeId),
                        claimId
                );
            }
        } catch (Exception exception) {
            log.error("[MANUAL_DEQUEUE_ORCHESTRATOR] Manual session-worker claim release failed | initiativeId={} claimId={}",
                    sanitizeString(initiativeId),
                    claimId,
                    exception
            );
        }
    }
}
