package it.gov.pagopa.ranker.service.ranker;

import com.azure.core.amqp.exception.AmqpException;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import it.gov.pagopa.ranker.config.RankerProcessorProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Getter
public class SessionWorker implements Runnable {

    private final String sessionId;
    private final String queueName;
    private final ServiceBusClientBuilder clientBuilder;
//    private final InitiativeBudgetService initiativeBudgetService;
    private final RankerProcessorProperties properties;
    private final Runnable onCompleted;

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    public SessionWorker(
            String sessionId, String queueName,
            ServiceBusClientBuilder clientBuilder,
            RankerProcessorProperties properties,
            Runnable onCompleted
    ) {
        this.sessionId = sessionId;
        this.queueName = queueName;
        this.clientBuilder = clientBuilder;
        this.properties = properties;
        this.onCompleted = onCompleted;
    }

    public void requestStop() {
        stopRequested.set(true);
    }

    @Override
    public void run() {
        int idleSeconds = 0;

        try (ServiceBusSessionReceiverClient sessionClient =
                     clientBuilder.sessionReceiver().queueName(queueName).buildClient()) {

            ServiceBusReceiverClient receiver;
            try {
                receiver = sessionClient.acceptSession(sessionId);
            } catch (AmqpException ex) {
                log.info("Session={}: lock denied, skip in the current cycle", sessionId);
                return;
            }

            try (receiver) {
//                if (!initiativeBudgetService.hasAvailableBudget(sessionId)) {
//                    log.info("Session={}: lock accepted but close because the initiative has not available budget", sessionId);
//                    return;
//                }
//
//                log.info("session={}: Start processing message, initiative has available budget", sessionId);
//
//                while (!stopRequested.get()) {
//                    if (!initiativeBudgetService.hasAvailableBudget(sessionId)) {
//                        log.info("Session={}: Stop processing message because the initiative has not budget", sessionId);
//                        break;
//                    }
//
//                    IterableStream<ServiceBusReceivedMessage> messages =
//                            receiver.receiveMessages(1, Duration.ofSeconds(5));
//
//                    ServiceBusReceivedMessage message = messages.stream().findFirst().orElse(null);
//
//                    if (message == null) {
//                        idleSeconds += 5;
//
//                        if (idleSeconds >= properties.getIdleTimeoutSeconds()) {
//                            log.info("Session={}: stop because idle timeout reached", sessionId);
//                            break;
//                        }
//
//                        continue;
//                    }
//
//                    idleSeconds = 0;
//
//                    try {
//                        //only poc: la logica reale dovrebbe aggiornare i contatore e preallocare il budget
//                        initiativeBudgetService.consumeOne(sessionId);
//
//                        log.info(
//                                "[PROCESS_MESSAGE_SESSION] Session={} sequenceNumber={} messageId={} body={} remainingBudget={}",
//                                sessionId,
//                                message.getSequenceNumber(),
//                                message.getMessageId(),
//                                message.getBody() != null ? message.getBody().toString() : "null",
//                                initiativeBudgetService.getRemainingBudget(sessionId)
//                        );
//
//                        receiver.complete(message);
//
//                    } catch (Exception ex) {
//                        log.error("Session={}: error processing messageId={}", sessionId, message.getMessageId(), ex);
//                        receiver.abandon(message);
//                    }
//                }
            }
        } catch (Exception ex) {
            log.error("Session={}: stop because unrecoverable error", sessionId, ex);
        } finally {
            onCompleted.run();
        }
    }
}