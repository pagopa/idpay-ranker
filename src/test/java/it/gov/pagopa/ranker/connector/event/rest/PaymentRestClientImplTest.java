package it.gov.pagopa.ranker.connector.event.rest;

import it.gov.pagopa.ranker.connector.rest.PaymentRestClientImpl;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class PaymentRestClientImplTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String TRANSACTION_ID = "trx123";
    private static final SyncTrxStatus STATUS = SyncTrxStatus.AUTHORIZED;

    private PaymentRestClientImpl paymentRestClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        paymentRestClient = new PaymentRestClientImpl(builder, BASE_URL);
    }

    @Test
    void existsByIdAndStatus_ReturnsTrue() {
        String expectedUri = BASE_URL + "/idpay/transactions/" + TRANSACTION_ID + "/status/" + STATUS + "/exists";

        mockServer.expect(requestTo(expectedUri))
                .andRespond(withSuccess("true", MediaType.APPLICATION_JSON));

        boolean result = paymentRestClient.existsByIdAndStatus(TRANSACTION_ID, STATUS);

        assertTrue(result);
        mockServer.verify();
    }

    @Test
    void existsByIdAndStatus_ReturnsFalse_WhenBodyIsFalse() {
        String expectedUri = BASE_URL + "/idpay/transactions/" + TRANSACTION_ID + "/status/" + STATUS + "/exists";

        mockServer.expect(requestTo(expectedUri))
                .andRespond(withSuccess("false", MediaType.APPLICATION_JSON));

        boolean result = paymentRestClient.existsByIdAndStatus(TRANSACTION_ID, STATUS);

        assertFalse(result);
        mockServer.verify();
    }

    @Test
    void existsByIdAndStatus_ReturnsFalse_WhenResponseBodyIsNull() {
        String expectedUri = BASE_URL + "/idpay/transactions/" + TRANSACTION_ID + "/status/" + STATUS + "/exists";

        mockServer.expect(requestTo(expectedUri))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        boolean result = paymentRestClient.existsByIdAndStatus(TRANSACTION_ID, STATUS);

        assertFalse(result);
        mockServer.verify();
    }
}