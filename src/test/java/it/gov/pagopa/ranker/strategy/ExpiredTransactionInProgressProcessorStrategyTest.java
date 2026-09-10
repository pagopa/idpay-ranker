package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.connector.rest.PaymentRestClient;
import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.utils.InitiativeCountersUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpiredTransactionInProgressProcessorStrategyTest {

    @Mock
    private InitiativeCountersService initiativeCountersServiceMock;

    @Mock
    private PaymentRestClient paymentRestClient;

    private ExpiredTransactionInProgressProcessorStrategy expiredTransactionInProgressProcessorStrategy;

    @BeforeEach
    public void init() {
        expiredTransactionInProgressProcessorStrategy = new ExpiredTransactionInProgressProcessorStrategy(
                paymentRestClient, initiativeCountersServiceMock);
    }

    @Test
    public void shouldReturnCapturedStatus() {
        Assertions.assertEquals(
                SyncTrxStatus.EXPIRED,
                expiredTransactionInProgressProcessorStrategy.getProcessedStatus()
        );
    }

    @Test
    public void shouldExecuteDecrementAndTrxRemovalIfUserIsMapped() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        String preallocationId = InitiativeCountersUtils.computePreallocationId(transactionInProgressDTO);
        doNothing().when(initiativeCountersServiceMock).updateInitiativeCounters(transactionInProgressDTO, preallocationId, transactionInProgressDTO.getId());
        when(paymentRestClient.existsByIdAndStatus("ID_1", SyncTrxStatus.EXPIRED))
                .thenReturn(true);
        Assertions.assertDoesNotThrow(() -> expiredTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));
        verify(initiativeCountersServiceMock).updateInitiativeCounters(any(),any(),any());

    }

    @Test
    public void shouldNotExecuteDecrementAndTrxRemovalIfUserIsNotMapped() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        when(paymentRestClient.existsByIdAndStatus("ID_1", SyncTrxStatus.EXPIRED))
                .thenReturn(false);
        Assertions.assertDoesNotThrow(() -> expiredTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));
        verify(paymentRestClient).existsByIdAndStatus(any(),any());
        verifyNoInteractions(initiativeCountersServiceMock);
    }

    @Test
    public void shouldNotExecuteUpdateIfPreallocationIsNotMapped() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        String preallocationId = InitiativeCountersUtils.computePreallocationId(transactionInProgressDTO);
        when(paymentRestClient.existsByIdAndStatus("ID_1", SyncTrxStatus.EXPIRED))
                .thenReturn(true);
        doNothing().when(initiativeCountersServiceMock).updateInitiativeCounters(transactionInProgressDTO, preallocationId, transactionInProgressDTO.getId());

        Assertions.assertDoesNotThrow(() -> expiredTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));
        verify(paymentRestClient).existsByIdAndStatus(any(),any());
        verify(initiativeCountersServiceMock).updateInitiativeCounters(any(),any(),any());
    }

    @Test
    public void shouldNotExecuteDeleteIfErrorOnCounterUpdate() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        String preallocationId = InitiativeCountersUtils.computePreallocationId(transactionInProgressDTO);
        when(paymentRestClient.existsByIdAndStatus("ID_1", SyncTrxStatus.EXPIRED))
                .thenReturn(true);
        doThrow(new RuntimeException("error")).when(initiativeCountersServiceMock)
                .updateInitiativeCounters(transactionInProgressDTO, preallocationId, transactionInProgressDTO.getId());

        Assertions.assertThrows(RuntimeException.class,
                () -> expiredTransactionInProgressProcessorStrategy.processTransaction(transactionInProgressDTO));
        verify(paymentRestClient).existsByIdAndStatus("ID_1", SyncTrxStatus.EXPIRED);
        verify(initiativeCountersServiceMock).updateInitiativeCounters(any(),any(),any());
        verifyNoMoreInteractions(paymentRestClient, initiativeCountersServiceMock);
    }

}
