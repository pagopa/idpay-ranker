package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.connector.rest.PaymentRestClient;
import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
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
    private InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;

    @Mock
    private InitiativeCountersRepository initiativeCountersRepository;

    @Mock
    private PaymentRestClient paymentRestClient;

    private ExpiredTransactionInProgressProcessorStrategy expiredTransactionInProgressProcessorStrategy;

    @BeforeEach
    public void init() {
        expiredTransactionInProgressProcessorStrategy = new ExpiredTransactionInProgressProcessorStrategy(
                paymentRestClient,
                initiativeCountersPreallocationsRepository,
                initiativeCountersRepository);
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
        when(initiativeCountersPreallocationsRepository.findByIdAndStatusThenUpdateStatus(
                preallocationId,
                PreallocationStatus.PREALLOCATED,
                PreallocationStatus.EXPIRED)).thenReturn(true);
        when(paymentRestClient.existsByIdAndStatus("ID_1", SyncTrxStatus.EXPIRED))
                .thenReturn(true);
        Assertions.assertDoesNotThrow(() -> expiredTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));
        verify(initiativeCountersPreallocationsRepository).findByIdAndStatusThenUpdateStatus(
                preallocationId,
                PreallocationStatus.PREALLOCATED,
                PreallocationStatus.EXPIRED);
        verify(initiativeCountersRepository).decrementOnboardedAndBudget("INIT_1", 1000L);

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
        verifyNoInteractions(initiativeCountersPreallocationsRepository, initiativeCountersRepository);
    }

    @Test
    public void shouldNotExecuteUpdateIfPreallocationAlreadyProcessed() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        String preallocationId = InitiativeCountersUtils.computePreallocationId(transactionInProgressDTO);
        when(paymentRestClient.existsByIdAndStatus("ID_1", SyncTrxStatus.EXPIRED))
                .thenReturn(true);
        when(initiativeCountersPreallocationsRepository.findByIdAndStatusThenUpdateStatus(
                preallocationId,
                PreallocationStatus.PREALLOCATED,
                PreallocationStatus.EXPIRED)).thenReturn(false);

        Assertions.assertDoesNotThrow(() -> expiredTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));
        verify(paymentRestClient).existsByIdAndStatus(any(),any());
        verify(initiativeCountersPreallocationsRepository).findByIdAndStatusThenUpdateStatus(
                preallocationId,
                PreallocationStatus.PREALLOCATED,
                PreallocationStatus.EXPIRED);
        verifyNoInteractions(initiativeCountersRepository);
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
        when(initiativeCountersPreallocationsRepository.findByIdAndStatusThenUpdateStatus(
                preallocationId,
                PreallocationStatus.PREALLOCATED,
                PreallocationStatus.EXPIRED)).thenReturn(true);
        doThrow(new RuntimeException("error")).when(initiativeCountersRepository)
                .decrementOnboardedAndBudget("INIT_1", 1000L);

        Assertions.assertThrows(RuntimeException.class,
                () -> expiredTransactionInProgressProcessorStrategy.processTransaction(transactionInProgressDTO));
        verify(paymentRestClient).existsByIdAndStatus("ID_1", SyncTrxStatus.EXPIRED);
        verify(initiativeCountersPreallocationsRepository).findByIdAndStatusThenUpdateStatus(
                preallocationId,
                PreallocationStatus.PREALLOCATED,
                PreallocationStatus.EXPIRED);
        verify(initiativeCountersRepository).decrementOnboardedAndBudget("INIT_1", 1000L);
        verifyNoMoreInteractions(paymentRestClient, initiativeCountersPreallocationsRepository, initiativeCountersRepository);
    }

}
