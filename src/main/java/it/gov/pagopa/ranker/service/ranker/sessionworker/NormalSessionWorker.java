package it.gov.pagopa.ranker.service.ranker.sessionworker;

import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import it.gov.pagopa.ranker.config.RankerProcessorProperties;
import it.gov.pagopa.ranker.enums.WorkerState;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import it.gov.pagopa.ranker.service.ranker.manualdequeue.ManualDequeueRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public final class NormalSessionWorker extends AbstractSessionWorker {

    /*
     * Manual dequeue requests handling fields.
     * Accessed only while holding manualRequestMonitor.
     */
    private final Object manualRequestMonitor = new Object();
    private volatile WorkerState workerState = WorkerState.NORMAL;
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
            if (isStopRequested()) {
                log.info("[NORMAL_SESSION_WORKER][MANUAL_DEQUEUE] Manual request rejected because worker is stopping | session={}",
                        sessionId);

                return false;
            }

            if (workerState != WorkerState.NORMAL) {
                log.info("[NORMAL_SESSION_WORKER][MANUAL_DEQUEUE] Manual request rejected because another manual request is active | session={} state={}",
                        sessionId, workerState);

                return false;
            }

            pendingManualRequest = request;
            workerState = WorkerState.MANUAL_ACTIVE;

            log.info("[NORMAL_SESSION_WORKER][MANUAL_DEQUEUE] Manual request queued | session={} requested={}",
                    sessionId, request.size());

            return true;
        }
    }

    private ManualDequeueRequest takePendingManualRequest() {
        synchronized (manualRequestMonitor) {
            /*
             * A request can be queued immediately before the worker begins
             * stopping. Do not claim it after stop has been requested.
             * onWorkerStopping() will clear it.
             */
            if (isStopRequested() || workerState != WorkerState.MANUAL_ACTIVE) {
                return null;
            }

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

        processMessage(receiver, message, "traceId");

        return 0;
    }

    private void executeManualRequest(
            ServiceBusReceiverClient receiver,
            ManualDequeueRequest request
    ) {
        log.info("[NORMAL_SESSION_WORKER][MANUAL_DEQUEUE] Normal worker will give leeway to a manual request with traceId={} | session={} requested={}",
                request.traceId(), sessionId, request.size());

        try {
            log.info("[NORMAL_SESSION_WORKER][MANUAL_DEQUEUE] Start session={} requestedMessages={} requestTraceId={}",
                    sessionId, request.size(), request.traceId());

            int processedMessages = processManualBatch(receiver, request.size(), request.traceId());

            log.info("[NORMAL_SESSION_WORKER][MANUAL_DEQUEUE] End session={} processedMessages={} requestedMessages={} stopRequested={} requestTraceId={}",
                    sessionId, processedMessages, request.size(), isStopRequested(), request.traceId());
        } catch (Exception ex) {
            log.error("[NORMAL_SESSION_WORKER][MANUAL_DEQUEUE] Manual processing failed; worker will stop | session={} requestTraceId={}",
                    sessionId, request.traceId(), ex);

            requestStop();
        } finally {
            synchronized (manualRequestMonitor) {
                workerState = WorkerState.NORMAL;
            }
        }
    }

    @Override
    protected void onWorkerStopping() {
        ManualDequeueRequest discardedManualRequest;

        synchronized (manualRequestMonitor) {
            discardedManualRequest = pendingManualRequest;
            pendingManualRequest = null;
            workerState = WorkerState.NORMAL;
        }

        if (discardedManualRequest != null) {
            log.info("[NORMAL_SESSION_WORKER][MANUAL_DEQUEUE] Pending manual request discarded because worker is stopping " +
                            "| session={} manualRequestTraceId={}",
                    sessionId, discardedManualRequest.traceId());
        }
    }
}
