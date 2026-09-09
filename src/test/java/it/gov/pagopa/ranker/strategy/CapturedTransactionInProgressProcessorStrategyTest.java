package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.connector.rest.PaymentRestClient;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapturedTransactionInProgressProcessorStrategyTest {

    @Mock
    private InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;

    @Mock
    private PaymentRestClient paymentRestClient;

    @Mock
    private InitiativeCountersRepository initiativeCountersRepositoryMock;

    private CapturedTransactionInProgressProcessorStrategy capturedTransactionInProgressProcessorStrategy;

    @BeforeEach
    void init() {
        Mockito.reset(initiativeCountersRepositoryMock, initiativeCountersRepositoryMock);
        capturedTransactionInProgressProcessorStrategy =
                new CapturedTransactionInProgressProcessorStrategy(
                        initiativeCountersPreallocationsRepository,
                        initiativeCountersRepositoryMock,
                        paymentRestClient);
    }


    @Test
     void shouldReturnCapturedStatus() {
        Assertions.assertEquals(
                SyncTrxStatus.CAPTURED,
                capturedTransactionInProgressProcessorStrategy.getProcessedStatus()
        );
    }

    @Test
     void shouldExecuteUpdateIfUserIsMapped() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setEffectiveAmountCents(1000L);
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setRewardCents(500L);
        transactionInProgressDTO.setUserId("USER_1");
        String preallocationId = InitiativeCountersUtils.computePreallocationId(transactionInProgressDTO);
        when(initiativeCountersPreallocationsRepository.findByIdAndStatusThenUpdateStatusToCaptured(preallocationId, PreallocationStatus.PREALLOCATED))
                .thenReturn(true);
        when(initiativeCountersRepositoryMock.updateCounterForCaptured("INIT_1",500L,1000L))
                .thenReturn(new InitiativeCounters());
        when(paymentRestClient.existsByIdAndStatus("ID_1",SyncTrxStatus.CAPTURED))
                .thenReturn(true);

        Assertions.assertDoesNotThrow(() -> capturedTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));

        verify(initiativeCountersPreallocationsRepository).findByIdAndStatusThenUpdateStatusToCaptured(preallocationId,PreallocationStatus.PREALLOCATED);
        verify(initiativeCountersRepositoryMock).updateCounterForCaptured("INIT_1",500L,1000L);

    }

    @Test
     void shouldNotExecuteUpdateIfUserIsNotMapped() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        when(paymentRestClient.existsByIdAndStatus("ID_1",SyncTrxStatus.CAPTURED))
                .thenReturn(false);
        Assertions.assertDoesNotThrow(() -> capturedTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));
        verify(paymentRestClient).existsByIdAndStatus(any(),any());
        verifyNoInteractions(initiativeCountersRepositoryMock);
        verifyNoInteractions(initiativeCountersPreallocationsRepository);
    }

    @Test
     void shouldNotExecuteUpdateIfPreallocationIsNotMapped() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        String preallocationId = InitiativeCountersUtils.computePreallocationId(transactionInProgressDTO);
        when(paymentRestClient.existsByIdAndStatus("ID_1",SyncTrxStatus.CAPTURED))
                .thenReturn(true);
        when(initiativeCountersPreallocationsRepository.findByIdAndStatusThenUpdateStatusToCaptured(preallocationId,PreallocationStatus.PREALLOCATED))
                .thenReturn(false);
        Assertions.assertDoesNotThrow(() -> capturedTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));
        verify(paymentRestClient).existsByIdAndStatus(any(),any());
        verify(initiativeCountersPreallocationsRepository).findByIdAndStatusThenUpdateStatusToCaptured(preallocationId,PreallocationStatus.PREALLOCATED);
        verifyNoInteractions(initiativeCountersRepositoryMock);
    }

    @Test
     void shouldThrowExceptionOnCounterUpdateError() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setEffectiveAmountCents(1000L);
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setRewardCents(500L);
        transactionInProgressDTO.setUserId("USER_1");
        String preallocationId = InitiativeCountersUtils.computePreallocationId(transactionInProgressDTO);
        when(initiativeCountersPreallocationsRepository.findByIdAndStatusThenUpdateStatusToCaptured(preallocationId,PreallocationStatus.PREALLOCATED))
                .thenReturn(true);
        when(paymentRestClient.existsByIdAndStatus("ID_1",SyncTrxStatus.CAPTURED))
                .thenReturn(true);
        when(initiativeCountersRepositoryMock
                        .updateCounterForCaptured("INIT_1",500L,1000L))
                .thenThrow(new RuntimeException("test"));
        Assertions.assertThrows(Exception.class, () ->
                capturedTransactionInProgressProcessorStrategy.processTransaction(transactionInProgressDTO));
        verify(initiativeCountersRepositoryMock).updateCounterForCaptured("INIT_1",500L,1000L);
    }


}
