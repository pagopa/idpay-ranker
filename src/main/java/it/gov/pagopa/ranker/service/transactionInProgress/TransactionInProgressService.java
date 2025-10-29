package it.gov.pagopa.ranker.service.transactionInProgress;

import org.springframework.messaging.Message;

public interface TransactionInProgressService {

    void processTransactionInProgressEH(Message<String> s);

}
