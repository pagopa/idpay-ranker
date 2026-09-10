package it.gov.pagopa.ranker.connector.rest;

import it.gov.pagopa.ranker.enums.SyncTrxStatus;

public interface PaymentRestClient {

    boolean existsByIdAndStatus(String transactionId, SyncTrxStatus status);
}

