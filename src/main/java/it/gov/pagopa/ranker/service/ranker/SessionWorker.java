package it.gov.pagopa.ranker.service.ranker;

import com.azure.core.amqp.exception.AmqpException;
import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import it.gov.pagopa.ranker.config.RankerProcessorProperties;
import it.gov.pagopa.ranker.exception.BudgetExhaustedException;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Getter
public class SessionWorker implements Runnable {

    private final String sessionId;
    private final String queueName;
    private final ServiceBusClientBuilder clientBuilder;
    private final InitiativeCountersService initiativeCountersService;
    private final RankerService rankerService;
    private final RankerProcessorProperties properties;
    private final Runnable onCompleted;

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    public SessionWorker(
            String sessionId, String queueName,
            ServiceBusClientBuilder clientBuilder,
            InitiativeCountersService initiativeCountersService, RankerService rankerService,
            RankerProcessorProperties properties,
            Runnable onCompleted
    ) {
        this.sessionId = sessionId;
        this.queueName = queueName;
        this.clientBuilder = clientBuilder;
        this.initiativeCountersService = initiativeCountersService;
        this.rankerService = rankerService;
        this.properties = properties;
        this.onCompleted = onCompleted;
    }

    public void requestStop() {
        stopRequested.set(true);
    }

    /**
     * Process Session work:
     * <li>Lock session</li>
     * <li>Check if the initiative associate has available budget</li>
     * <li>Process messages for initiative</li>
     */
    @Override
    public void run() {
        int idleSeconds = 0;

        try (ServiceBusSessionReceiverClient sessionClient =
                     clientBuilder.sessionReceiver().queueName(queueName).buildClient()) {

            ServiceBusReceiverClient receiver = lockSession(sessionClient);

            if (!initiativeCountersService.hasAvailableBudget(sessionId)) {
                log.info("[SESSION_WORKER][CHECK_BUDGET] Stop processing message because the initiative for session {} has not budget", sessionId);
                return;
            }

            log.info("[SESSION_WORKER][STARTER_CONSUMER] Start processing message for session {}", sessionId);

            execute(receiver, idleSeconds);

        } catch (Exception ex) {
            log.error("[SESSION_WORKER][STOP_CONSUMER] Stop session {} because unrecoverable error", sessionId, ex);
        } finally {
            onCompleted.run();
        }
    }

    private void execute(ServiceBusReceiverClient receiver, int idleSeconds) {
        while (!stopRequested.get()) {
            IterableStream<ServiceBusReceivedMessage> messages =
                    receiver.receiveMessages(1, Duration.ofSeconds(properties.getWaitMessageSeconds()));

            ServiceBusReceivedMessage message = messages.stream().findFirst().orElse(null);

            if (message == null) {
                idleSeconds += properties.getWaitMessageSeconds();

                if (idleSeconds >= properties.getIdleTimeoutSeconds()) {
                    log.info("[SESSION_WORKER][TIMEOUT] Stop processing message because the initiative for session {} idle timeout reached", sessionId);
                    requestStop();
                }

                continue;
            }

            idleSeconds = 0;

            processMessage(receiver, message);
        }
    }

    private ServiceBusReceiverClient lockSession(ServiceBusSessionReceiverClient sessionClient){
        try {
            return sessionClient.acceptSession(sessionId);
        } catch (AmqpException ex) {
            log.error("[SESSION_WORKER][LOCK_SESSION] Lock denied for session {}, skip in the current cycle", sessionId);
            throw ex;
        }
    }

    private void processMessage(ServiceBusReceiverClient receiver, ServiceBusReceivedMessage message) {
        try {
            log.info("[SESSION_WORKER][RECEIVE_MESSAGE] Receive message for initiative with session {}: sequenceNumber={} messageId={} body={}",
                    sessionId, message.getSequenceNumber(), message.getMessageId(), message.getBody());
            rankerService.execute(message);
            receiver.complete(message);
        } catch (BudgetExhaustedException budgetExhaustedException) {
            log.error("[SESSION_WORKER][BUDGET_CONTEXT] Budget exhausted for initiative with.", budgetExhaustedException);
            receiver.abandon(message);
            requestStop();
        } catch (Exception ex) {
            log.error("[SESSION_WORKER][MESSAGE_CONTEXT] Error processing message {} for session {}", sessionId, message.getMessageId(), ex);
            receiver.abandon(message);
        }
    }
}