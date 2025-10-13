package it.gov.pagopa.utils;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersServiceImpl;

public class InitiativeCountersUtils {

    public static String computePreallocationId(TransactionInProgressDTO transactionInProgress) {
        return transactionInProgress.getUserId() + InitiativeCountersServiceImpl.ID_SEPARATOR + transactionInProgress.getInitiativeId();
    }

}
