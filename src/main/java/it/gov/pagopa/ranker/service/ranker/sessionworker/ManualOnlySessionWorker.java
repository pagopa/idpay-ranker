package it.gov.pagopa.ranker.service.ranker.sessionworker;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import it.gov.pagopa.ranker.config.RankerProcessorProperties;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public final class ManualOnlySessionWorker extends AbstractSessionWorker {

    private final int requestedMessages;

    public ManualOnlySessionWorker(
            String sessionId,
            String queueName,
            ServiceBusClientBuilder clientBuilder,
            InitiativeCountersService initiativeCountersService,
            RankerService rankerService,
            RankerProcessorProperties properties,
            int requestedMessages,
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

        if (requestedMessages < 1) {
            throw new IllegalArgumentException(
                    "[MANUAL_ONLY_SESSION_WORKER] requestedMessages must be greater than zero"
            );
        }

        this.requestedMessages = requestedMessages;
    }

    @Override
    protected void execute(ServiceBusReceiverClient receiver) {
        log.info("[MANUAL_ONLY_SESSION_WORKER][START] session={} requested={}",
                sessionId, requestedMessages);

        int processedMessages = processManualBatch(receiver, requestedMessages);

        log.info("[MANUAL_ONLY_SESSION_WORKER][END] session={} processed={} requested={}",
                sessionId, processedMessages, requestedMessages);
    }
}
