package it.gov.pagopa.ranker.service.ranker.sessionworker;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import it.gov.pagopa.ranker.config.RankerProcessorProperties;
import it.gov.pagopa.ranker.exception.ManualDequeueSizeNotValidException;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static it.gov.pagopa.ranker.constants.ErrorMessages.INVALID_SIZE_NOT_POSITIVE;
import static it.gov.pagopa.utils.CommonUtils.getSpanContextTraceId;
import static it.gov.pagopa.utils.CommonUtils.sanitizeString;

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
            int requestedMessages
    ) {
        if (requestedMessages < 1) {
            log.error("[MANUAL_ONLY_SESSION_WORKER] Error when calling manualDequeue for initiativeId={} | size is not positive, its value is: {}",
                    sanitizeString(sessionId), requestedMessages);
            throw new ManualDequeueSizeNotValidException(INVALID_SIZE_NOT_POSITIVE);
        }

        super(
                sessionId,
                queueName,
                clientBuilder,
                initiativeCountersService,
                rankerService,
                properties,
                () -> {}
        );

        this.requestedMessages = requestedMessages;
    }

    @Override
    protected void execute(ServiceBusReceiverClient receiver) {
        log.info("[MANUAL_ONLY_SESSION_WORKER][START] session={} requested={}",
                sanitizeString(sessionId), requestedMessages);

        int processedMessages = processManualBatch(receiver, requestedMessages, getSpanContextTraceId());

        log.info("[MANUAL_ONLY_SESSION_WORKER][END] session={} processed={} requested={} stopRequested={}",
                sanitizeString(sessionId), processedMessages, requestedMessages, isStopRequested());
    }
}
