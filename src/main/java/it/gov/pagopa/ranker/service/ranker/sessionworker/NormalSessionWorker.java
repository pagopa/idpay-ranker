package it.gov.pagopa.ranker.service.ranker.sessionworker;

import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import it.gov.pagopa.ranker.config.RankerProcessorProperties;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import it.gov.pagopa.ranker.service.ranker.manualdequeue.ManualDequeueRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.time.Duration;

import static it.gov.pagopa.ranker.constants.RankerConstants.MDC_TRACE_ID;

@Slf4j
@Getter
public final class NormalSessionWorker extends AbstractSessionWorker {

    // Manual dequeue requests handling fields.
    private final Object manualRequestMonitor = new Object();
    private boolean acceptingManualRequests = true;
    private ManualDequeueRequest pendingManualRequest;

    public NormalSessionWorker(
            String sessionId,
            String queueName,
            ServiceBusClientBuilder clientBuilder,
            InitiativeCountersService initiativeCountersService,
            RankerService rankerService,
            RankerProcessorProperties properties,
            Runnable onCompleted
    ) {
        super(
                sessionId,
                queueName,
                clientBuilder,
                initiativeCountersService,
                rankerService,
                properties,
                onCompleted
        );
    }

    public boolean tryEnqueueManualRequest(ManualDequeueRequest request) {
        synchronized (manualRequestMonitor) {
            if (!acceptingManualRequests) {
                log.info("[NORMAL_SESSION_WORKER][MANUAL_DEQUEUE] Worker is stopping; manual request rejected for session {}",
                        sessionId);

                return false;
            }

            if (pendingManualRequest != null) {
                log.info("[NORMAL_SESSION_WORKER][MANUAL_DEQUEUE] A manual request is already pending for session {}",
                        sessionId);

                return false;
            }

            pendingManualRequest = request;

            log.info("[NORMAL_SESSION_WORKER][MANUAL_DEQUEUE] Manual request queued on NormalSessionWorker for session {}",
                    sessionId);

            return true;
        }
    }

    private ManualDequeueRequest takePendingManualRequest() {
        synchronized (manualRequestMonitor) {
            ManualDequeueRequest request = pendingManualRequest;
            pendingManualRequest = null;

            return request;
        }
    }

    @Override
    protected void execute(ServiceBusReceiverClient receiver) {
        int idleSeconds = 0;

        while (!isStopRequested()) {
            ManualDequeueRequest manualRequest = takePendingManualRequest();

            if (manualRequest != null) {
                executeManualRequest(receiver, manualRequest);
                idleSeconds = 0;
                continue;
            }

            idleSeconds = processNormalIteration(receiver, idleSeconds);
        }
    }

    private int processNormalIteration(
            ServiceBusReceiverClient receiver,
            int idleSeconds
    ) {
        IterableStream<ServiceBusReceivedMessage> messages =
                receiver.receiveMessages(1, Duration.ofSeconds(properties.getWaitMessageSeconds()));

        ServiceBusReceivedMessage message = messages.stream().findFirst().orElse(null);

        if (message == null) {
            int updatedIdleSeconds = idleSeconds + properties.getWaitMessageSeconds();

            if (updatedIdleSeconds >= properties.getIdleTimeoutSeconds()) {
                log.info(
                        "[NORMAL_SESSION_WORKER][TIMEOUT] Idle timeout for session {}",
                        sessionId
                );

                requestStop();
            }

            return updatedIdleSeconds;
        }

        processMessage(receiver, message);

        return 0;
    }

    private void executeManualRequest(
            ServiceBusReceiverClient receiver,
            ManualDequeueRequest request
    ) {
        String previousTraceId = MDC.get(MDC_TRACE_ID);

        try {
            setMdcTraceId(request.traceId());

            log.info("[NORMAL_SESSION_WORKER][MANUAL_DEQUEUE] Start session={} requested={}",
                    sessionId, request.size());

            int processedMessages = processManualBatch(receiver, request.size());

            log.info("[NORMAL_SESSION_WORKER][MANUAL_DEQUEUE] End session={} processed={} requested={}",
                    sessionId, processedMessages, request.size());
        } catch (Exception ex) {
            log.error("[NORMAL_SESSION_WORKER][MANUAL_DEQUEUE] Manual processing failed; worker will stop | session={}",
                    sessionId, ex);

            requestStop();
        } finally {
            try {
                releaseManualRequest(request);
            } finally {
                setMdcTraceId(previousTraceId);
            }
        }
    }

    @Override
    protected void onWorkerStopping() {
        ManualDequeueRequest pendingRequest;

        synchronized (manualRequestMonitor) {
            /*
             * From now on, no new request can be accepted.
             */
            acceptingManualRequests = false;

            /*
             * If a request was queued but not yet claimed by the normal worker,
             * shutdown owns it and must release the orchestrator reservation.
             */
            pendingRequest = pendingManualRequest;
            pendingManualRequest = null;
        }

        /*
         * Callback must run outside the monitor.
         */
        if (pendingRequest != null) {
            releaseManualRequest(pendingRequest);
        }
    }

    private void releaseManualRequest(ManualDequeueRequest request) {
        try {
            request.onCompleted().run();
        } catch (Exception ex) {
            log.error("[NORMAL_SESSION_WORKER][MANUAL_DEQUEUE] Failed to release pending manual request for session {}",
                    sessionId, ex);
        }
    }

    private void setMdcTraceId(String traceId) {
        if (traceId == null) {
            MDC.remove(MDC_TRACE_ID);
        } else {
            MDC.put(MDC_TRACE_ID, traceId);
        }
    }
}
