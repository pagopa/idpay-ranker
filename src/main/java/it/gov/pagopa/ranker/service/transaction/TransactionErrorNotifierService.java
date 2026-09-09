package it.gov.pagopa.ranker.service.transaction;

import it.gov.pagopa.common.config.KafkaConfiguration;
import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import org.springframework.messaging.Message;

public interface TransactionErrorNotifierService {
    boolean notifyExpiredTransaction(Message<?> message, String description, boolean retryable, Throwable exception);

    boolean notify(KafkaConfiguration.BaseKafkaInfoDTO kafkaInfoDTO, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception);

    Message<TransactionInProgressDTO> buildMessage(TransactionInProgressDTO trx, String key);

}
