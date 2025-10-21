package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;

public interface TransactionInProgressProcessorStrategy {

    SyncTrxStatus getProcessedStatus();

    void processTransaction(TransactionInProgressDTO transactionInProgress);

}
