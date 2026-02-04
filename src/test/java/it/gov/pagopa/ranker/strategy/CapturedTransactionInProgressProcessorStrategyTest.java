package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.repository.TransactionInProgressRepository;
import it.gov.pagopa.utils.InitiativeCountersUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static it.gov.pagopa.ranker.strategy.CapturedTransactionInProgressProcessorStrategy.PREALLOCATED;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CapturedTransactionInProgressProcessorStrategyTest {

    @Mock
    private InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;

    @Mock
    private TransactionInProgressRepository transactionInProgressRepositoryMock;

    @Mock
    private InitiativeCountersRepository initiativeCountersRepositoryMock;

    private CapturedTransactionInProgressProcessorStrategy capturedTransactionInProgressProcessorStrategy;

    @BeforeEach
    public void init() {
        Mockito.reset(initiativeCountersRepositoryMock, initiativeCountersRepositoryMock);
        capturedTransactionInProgressProcessorStrategy =
                new CapturedTransactionInProgressProcessorStrategy(
                        initiativeCountersPreallocationsRepository,
                        initiativeCountersRepositoryMock,
                        transactionInProgressRepositoryMock);
    }


    @Test
    public void shouldReturnCapturedStatus() {
        Assertions.assertEquals(
                SyncTrxStatus.CAPTURED,
                capturedTransactionInProgressProcessorStrategy.getProcessedStatus()
        );
    }

    @Test
    public void shouldExecuteUpdateIfUserIsMapped() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setEffectiveAmountCents(1000L);
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setRewardCents(500L);
        transactionInProgressDTO.setUserId("USER_1");
        String preallocationId = InitiativeCountersUtils.computePreallocationId(transactionInProgressDTO);
        when(initiativeCountersPreallocationsRepository.findByIdAndStatusThenUpdateStatusToCaptured(eq(preallocationId),eq(PREALLOCATED)))
                .thenReturn(true);
        when(initiativeCountersRepositoryMock.updateCounterForCaptured("INIT_1",500L,1000L))
                .thenReturn(new InitiativeCounters());
        when(transactionInProgressRepositoryMock.existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.CAPTURED)))
                .thenReturn(true);

        Assertions.assertDoesNotThrow(() -> capturedTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));

        verify(initiativeCountersPreallocationsRepository).findByIdAndStatusThenUpdateStatusToCaptured(eq(preallocationId),eq(PREALLOCATED));
        verify(initiativeCountersRepositoryMock).updateCounterForCaptured("INIT_1",500L,1000L);

    }

    @Test
    public void shouldNotExecuteUpdateIfUserIsNotMapped() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        when(transactionInProgressRepositoryMock.existsByIdAndStatus("ID_1",SyncTrxStatus.CAPTURED))
                .thenReturn(false);
        Assertions.assertDoesNotThrow(() -> capturedTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));
        verify(transactionInProgressRepositoryMock).existsByIdAndStatus(any(),any());
        verifyNoInteractions(initiativeCountersRepositoryMock);
        verifyNoInteractions(initiativeCountersPreallocationsRepository);
    }

    @Test
    public void shouldNotExecuteUpdateIfPreallocationIsNotMapped() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        String preallocationId = InitiativeCountersUtils.computePreallocationId(transactionInProgressDTO);
        when(transactionInProgressRepositoryMock.existsByIdAndStatus("ID_1",SyncTrxStatus.CAPTURED))
                .thenReturn(true);
        when(initiativeCountersPreallocationsRepository.findByIdAndStatusThenUpdateStatusToCaptured(eq(preallocationId),eq(PREALLOCATED)))
                .thenReturn(false);
        Assertions.assertDoesNotThrow(() -> capturedTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));
        verify(transactionInProgressRepositoryMock).existsByIdAndStatus(any(),any());
        verify(initiativeCountersPreallocationsRepository).findByIdAndStatusThenUpdateStatusToCaptured(eq(preallocationId),eq(PREALLOCATED));
        verifyNoInteractions(initiativeCountersRepositoryMock);
    }

    @Test
    public void shouldThrowExceptionOnCounterUpdateError() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setEffectiveAmountCents(1000L);
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setRewardCents(500L);
        transactionInProgressDTO.setUserId("USER_1");
        String preallocationId = InitiativeCountersUtils.computePreallocationId(transactionInProgressDTO);
        when(initiativeCountersPreallocationsRepository.findByIdAndStatusThenUpdateStatusToCaptured(eq(preallocationId),eq(PREALLOCATED)))
                .thenReturn(true);
        when(transactionInProgressRepositoryMock.existsByIdAndStatus("ID_1",SyncTrxStatus.CAPTURED))
                .thenReturn(true);
        when(initiativeCountersRepositoryMock
                        .updateCounterForCaptured("INIT_1",500L,1000L))
                .thenThrow(new RuntimeException("test"));
        Assertions.assertThrows(Exception.class, () ->
                capturedTransactionInProgressProcessorStrategy.processTransaction(transactionInProgressDTO));
        verify(initiativeCountersRepositoryMock).updateCounterForCaptured("INIT_1",500L,1000L);
    }


}
