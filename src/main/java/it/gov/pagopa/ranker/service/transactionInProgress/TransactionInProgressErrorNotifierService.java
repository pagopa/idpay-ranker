package it.gov.pagopa.ranker.service.transactionInProgress;

import it.gov.pagopa.common.config.KafkaConfiguration;
import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.domain.model.TransactionInProgress;
import org.springframework.messaging.Message;

public interface TransactionInProgressErrorNotifierService {
    boolean notifyExpiredTransaction(Message<?> message, String description, boolean retryable, Throwable exception);

    boolean notify(KafkaConfiguration.BaseKafkaInfoDTO kafkaInfoDTO, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception);

    Message<TransactionInProgressDTO> buildMessage(TransactionInProgressDTO trx, String key);

}
