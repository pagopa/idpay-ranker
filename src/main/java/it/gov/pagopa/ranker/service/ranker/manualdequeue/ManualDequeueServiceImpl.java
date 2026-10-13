package it.gov.pagopa.ranker.service.ranker.manualdequeue;

import it.gov.pagopa.ranker.domain.dto.ManualDequeueResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static it.gov.pagopa.ranker.constants.RankerConstants.MDC_TRACE_ID;
import static it.gov.pagopa.ranker.constants.RankerConstants.STATUS_ACCEPTED;

@Slf4j
@Service
public class ManualDequeueServiceImpl implements ManualDequeueService {

    private final Executor manualDequeueOrchestratorExecutor;
    private final ManualDequeueOrchestrator manualDequeueOrchestrator;
    private final Set<String> manuallyOwnedInitiatives = ConcurrentHashMap.newKeySet();

    public ManualDequeueServiceImpl(@Qualifier("manualDequeueOrchestratorExecutor")
                                    Executor manualDequeueOrchestratorExecutor,
                                    ManualDequeueOrchestrator manualDequeueOrchestrator) {
        this.manualDequeueOrchestratorExecutor = manualDequeueOrchestratorExecutor;
        this.manualDequeueOrchestrator = manualDequeueOrchestrator;
    }

    @Override
    public ManualDequeueResponseDTO requestDequeue(String initiativeId, int size) {
        if (size < 1) {
            throw new IllegalArgumentException("[MANUAL_DEQUEUE] Size must be greater than zero");
        }

        // Extract current context traceId.
        String traceId = MDC.get(MDC_TRACE_ID);

        // Build response to return.
        ManualDequeueResponseDTO response = ManualDequeueResponseDTO.builder()
                .initiativeId(initiativeId)
                .requestedMessages(size)
                .status(STATUS_ACCEPTED)
                .traceId(MDC.get(MDC_TRACE_ID))
                .build();

        /*
         * Atomic check-and-reserve.
         *
         * false: another manual dequeue for this initiative is already queued
         *        or processing, so do not submit another executor task.
         * true:  this request owns the manual-dequeue slot.
         */
        if (!manuallyOwnedInitiatives.add(initiativeId)) {
            log.info("[MANUAL_DEQUEUE] Initiative {} already has a manual dequeue in progress.", initiativeId);
            return response;
        }

        // Build request to send.
        ManualDequeueRequest request = new ManualDequeueRequest(
                initiativeId,
                size,
                traceId,
                () -> releaseManualOwnership(initiativeId)
        );

        try {
            manualDequeueOrchestratorExecutor.execute(
                    () -> manualDequeueOrchestrator.orchestrate(request)
            );
        } catch (RuntimeException exception) {
            /*
             * The task was never submitted, so no worker exists and its
             * onCompleted callback cannot release the reservation.
             */
            releaseManualOwnership(initiativeId);

            log.error("[MANUAL_DEQUEUE] Failed to submit manual dequeue for initiative {}",
                    initiativeId, exception);
        }

        return response;
    }

    private void releaseManualOwnership(String initiativeId) {
        boolean removed = manuallyOwnedInitiatives.remove(initiativeId);

        if (removed) {
            log.info("[MANUAL_DEQUEUE] Released manual ownership for initiative {}",
                    initiativeId);
        } else {
            log.debug("[MANUAL_DEQUEUE] Manual ownership was already released for initiative {}",
                    initiativeId);
        }
    }

}
