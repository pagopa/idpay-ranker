package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.repository.TransactionInProgressRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersServiceImpl;
import it.gov.pagopa.utils.InitiativeCountersUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpiredTransactionInProgressProcessorStrategyTest {

    @Mock
    private InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;

    @Mock
    private TransactionInProgressRepository transactionInProgressRepositoryMock;

    @Mock
    private InitiativeCountersRepository initiativeCountersRepositoryMock;

    private ExpiredTransactionInProgressProcessorStrategy expiredTransactionInProgressProcessorStrategy;

    @BeforeEach
    public void init() {
        Mockito.reset(initiativeCountersRepositoryMock, initiativeCountersRepositoryMock);
        expiredTransactionInProgressProcessorStrategy = new ExpiredTransactionInProgressProcessorStrategy(
                initiativeCountersPreallocationsRepository, initiativeCountersRepositoryMock,
                transactionInProgressRepositoryMock);
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
        when(initiativeCountersPreallocationsRepository.existsById(eq(preallocationId)))
                .thenReturn(true);
        when(initiativeCountersRepositoryMock.decrementOnboardedAndBudget(eq("INIT_1"),eq(1000L)))
                .thenReturn(new InitiativeCounters());
        when(transactionInProgressRepositoryMock.existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.EXPIRED)))
                .thenReturn(true);
        Assertions.assertDoesNotThrow(() -> expiredTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));
        verify(initiativeCountersPreallocationsRepository).existsById(eq(preallocationId));
        verify(initiativeCountersRepositoryMock).decrementOnboardedAndBudget(eq("INIT_1"),eq(1000L));
        verify(transactionInProgressRepositoryMock).deleteById(eq(transactionInProgressDTO.getId()));

    }

    @Test
    public void shouldNotExecuteDecrementAndTrxRemovalIfUserIsNotMapped() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        when(transactionInProgressRepositoryMock.existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.EXPIRED)))
                .thenReturn(false);
        Assertions.assertDoesNotThrow(() -> expiredTransactionInProgressProcessorStrategy
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
        when(transactionInProgressRepositoryMock.existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.EXPIRED)))
                .thenReturn(true);
        when(initiativeCountersPreallocationsRepository.existsById(eq(preallocationId)))
                .thenReturn(false);
        Assertions.assertDoesNotThrow(() -> expiredTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));
        verify(transactionInProgressRepositoryMock).existsByIdAndStatus(any(),any());
        verify(initiativeCountersPreallocationsRepository).existsById(eq(preallocationId));
        verifyNoInteractions(initiativeCountersRepositoryMock);
    }

    @Test
    public void shouldNotExecuteDeleteIfErrorOnCounterUpdate() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        String preallocationId = InitiativeCountersUtils.computePreallocationId(transactionInProgressDTO);
        when(transactionInProgressRepositoryMock.existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.EXPIRED)))
                .thenReturn(true);
        when(initiativeCountersPreallocationsRepository.existsById(eq(preallocationId)))
                .thenReturn(true);
        when(initiativeCountersRepositoryMock.decrementOnboardedAndBudget(eq("INIT_1"),eq(1000L)))
                .thenThrow(new RuntimeException("error"));
        Assertions.assertThrows(RuntimeException.class,
                () -> expiredTransactionInProgressProcessorStrategy.processTransaction(transactionInProgressDTO));
        verify(transactionInProgressRepositoryMock).existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.EXPIRED));
        verify(initiativeCountersPreallocationsRepository).existsById(eq(preallocationId));
        verify(initiativeCountersRepositoryMock).decrementOnboardedAndBudget(eq("INIT_1"),eq(1000L));
        verifyNoMoreInteractions(transactionInProgressRepositoryMock);
    }

    @Test
    public void shouldThrowExceptionOnDeleteErrorOnCounterUpdate() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        String preallocationId = InitiativeCountersUtils.computePreallocationId(transactionInProgressDTO);
        when(transactionInProgressRepositoryMock.existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.EXPIRED)))
                .thenReturn(true);
        when(initiativeCountersPreallocationsRepository.existsById(eq(preallocationId)))
                .thenReturn(true);
        when(initiativeCountersRepositoryMock.decrementOnboardedAndBudget(eq("INIT_1"),eq(1000L)))
                .thenReturn(new InitiativeCounters());
        doThrow(new RuntimeException("error")).doNothing().when(transactionInProgressRepositoryMock)
                .deleteById(eq("ID_1"));
        Assertions.assertThrows(Exception.class, () ->
                expiredTransactionInProgressProcessorStrategy.processTransaction(transactionInProgressDTO));
        verify(initiativeCountersPreallocationsRepository).existsById(eq(preallocationId));
        verify(initiativeCountersRepositoryMock).decrementOnboardedAndBudget(eq("INIT_1"),eq(1000L));
        verify(transactionInProgressRepositoryMock).deleteById(eq(transactionInProgressDTO.getId()));
    }


}
