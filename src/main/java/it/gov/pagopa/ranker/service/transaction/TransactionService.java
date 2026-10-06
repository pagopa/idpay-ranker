package it.gov.pagopa.ranker.service.transaction;

import org.springframework.messaging.Message;

public interface TransactionService {

    void execute(Message<String> transactionMessage);

}
