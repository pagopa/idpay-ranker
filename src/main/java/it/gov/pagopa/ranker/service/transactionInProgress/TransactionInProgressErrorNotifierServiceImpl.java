package it.gov.pagopa.ranker.service.transactionInProgress;

import it.gov.pagopa.common.config.KafkaConfiguration;
import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TransactionInProgressErrorNotifierServiceImpl implements TransactionInProgressErrorNotifierService {
    private static final String KAFKA_BINDINGS_TRANSACTIONS = "rewardTrxConsumer-in-0";

    private final ErrorNotifierService errorNotifierService;
    private final KafkaConfiguration kafkaConfiguration;

    public TransactionInProgressErrorNotifierServiceImpl(ErrorNotifierService errorNotifierService,
                                           KafkaConfiguration kafkaConfiguration) {
        this.errorNotifierService = errorNotifierService;

        this.kafkaConfiguration = kafkaConfiguration;
    }

    @Override
    public boolean notifyExpiredTransaction(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(kafkaConfiguration.getStream().getBindings().get(KAFKA_BINDINGS_TRANSACTIONS), message, description, retryable, false, exception);
    }

    @Override
    public boolean notify(KafkaConfiguration.BaseKafkaInfoDTO kafkaInfoDTO, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception) {
        return errorNotifierService.notify(kafkaInfoDTO, message, description, retryable, resendApplication, exception);
    }

    @Override
    public Message<TransactionInProgressDTO> buildMessage(TransactionInProgressDTO trx, String key) {
        return MessageBuilder.withPayload(trx)
                .setHeader(KafkaHeaders.KEY, key)
                .build();
    }

}
