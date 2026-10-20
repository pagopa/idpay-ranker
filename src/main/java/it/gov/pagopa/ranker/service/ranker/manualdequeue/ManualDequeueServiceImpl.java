package it.gov.pagopa.ranker.service.ranker.manualdequeue;

import it.gov.pagopa.ranker.domain.dto.ManualDequeueResponseDTO;
import it.gov.pagopa.ranker.exception.ManualDequeueSizeNotValidException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

import static it.gov.pagopa.ranker.constants.ErrorMessages.INVALID_SIZE_NOT_POSITIVE;
import static it.gov.pagopa.ranker.constants.RankerConstants.MDC_TRACE_ID;
import static it.gov.pagopa.ranker.constants.RankerConstants.STATUS_ACCEPTED;

@Slf4j
@Service
public class ManualDequeueServiceImpl implements ManualDequeueService {

    private final Executor manualDequeueOrchestratorExecutor;
    private final ManualDequeueOrchestrator manualDequeueOrchestrator;

    public ManualDequeueServiceImpl(@Qualifier("manualDequeueOrchestratorExecutor")
                                    Executor manualDequeueOrchestratorExecutor,
                                    ManualDequeueOrchestrator manualDequeueOrchestrator) {
        this.manualDequeueOrchestratorExecutor = manualDequeueOrchestratorExecutor;
        this.manualDequeueOrchestrator = manualDequeueOrchestrator;
    }

    @Override
    public ManualDequeueResponseDTO requestDequeue(String initiativeId, int size) {
        if (size < 1) {
            log.error("[MANUAL_DEQUEUE_SERVICE] Error when calling manualDequeue | size is not positive, its value is: {}", size);
            throw new ManualDequeueSizeNotValidException(INVALID_SIZE_NOT_POSITIVE);
        }

        // Extract current context traceId.
        String traceId = MDC.get(MDC_TRACE_ID);

        // Build response to return.
        ManualDequeueResponseDTO response = buildDequeueResponse(initiativeId, size, traceId);

        // Build request to send.
        ManualDequeueRequest request = buildDequeueRequest(initiativeId, size, traceId);

        try {
            manualDequeueOrchestratorExecutor.execute(
                    () -> manualDequeueOrchestrator.orchestrate(request)
            );

            log.info("[MANUAL_DEQUEUE_SERVICE] Manual dequeue orchestration submitted | initiativeId={} requested={}",
                    initiativeId, size);
        } catch (Exception ex) {
            log.error("[MANUAL_DEQUEUE_SERVICE] Failed to submit manual dequeue orchestration | initiativeId={} requested={}",
                    initiativeId, size, ex);
        }

        return response;
    }

    private ManualDequeueResponseDTO buildDequeueResponse(String initiativeId, int size, String traceId) {
        return ManualDequeueResponseDTO.builder()
                .initiativeId(initiativeId)
                .requestedMessages(size)
                .status(STATUS_ACCEPTED)
                .traceId(traceId)
                .build();
    }

    private ManualDequeueRequest buildDequeueRequest(String initiativeId, int size, String traceId) {
        return new ManualDequeueRequest(
                initiativeId,
                size,
                traceId
        );
    }
}
