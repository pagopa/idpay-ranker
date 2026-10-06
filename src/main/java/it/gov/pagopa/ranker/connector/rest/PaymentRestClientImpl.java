package it.gov.pagopa.ranker.connector.rest;

import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static it.gov.pagopa.utils.CommonUtils.sanitizeString;

@Component
@Slf4j
public class PaymentRestClientImpl implements PaymentRestClient {

    private static final String TRANSACTION_STATUS_CHECK_PATH = "/idpay/transactions/{transactionId}/status/{status}/exists";

    private final RestClient restClient;

    public PaymentRestClientImpl(
            RestClient.Builder restClientBuilder,
            @Value("${rest-client.payment.base-url}") String paymentBaseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(paymentBaseUrl).build();
    }

    @Override
    public boolean existsByIdAndStatus(String transactionId, SyncTrxStatus status) {
        Boolean exists = restClient.get()
                .uri(TRANSACTION_STATUS_CHECK_PATH, transactionId, status)
                .retrieve()
                .body(Boolean.class);

        boolean result = Boolean.TRUE.equals(exists);
        log.debug("[PAYMENT_REST_CLIENT] transactionId={} status={} exists={}",
                sanitizeString(transactionId), status, result);
        return result;
    }
}

