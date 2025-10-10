package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.repository.TransactionInProgressRepository;
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
    private TransactionInProgressRepository transactionInProgressRepositoryMock;

    @Mock
    private InitiativeCountersRepository initiativeCountersRepositoryMock;

    private ExpiredTransactionInProgressProcessorStrategy expiredTransactionInProgressProcessorStrategy;

    @BeforeEach
    public void init() {
        Mockito.reset(initiativeCountersRepositoryMock, initiativeCountersRepositoryMock);
        expiredTransactionInProgressProcessorStrategy = new ExpiredTransactionInProgressProcessorStrategy(initiativeCountersRepositoryMock, transactionInProgressRepositoryMock);
    }


    @Test
    public void shouldExecuteDecrementAndTrxRemovalIfUserIsMapped() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        when(initiativeCountersRepositoryMock.existsByInitiativeIdAndUserId(eq("INIT_1"), eq("USER_1")))
                .thenReturn(true);
        when(initiativeCountersRepositoryMock.decrementOnboardedAndBudget(eq("INIT_1"),eq("USER_1"),eq(1000L)))
                .thenReturn(new InitiativeCounters());
        when(transactionInProgressRepositoryMock.existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.EXPIRED))).thenReturn(true);
        Assertions.assertDoesNotThrow(() -> expiredTransactionInProgressProcessorStrategy.processTransaction(transactionInProgressDTO));
        verify(initiativeCountersRepositoryMock).existsByInitiativeIdAndUserId(eq("INIT_1"),eq("USER_1"));
        verify(initiativeCountersRepositoryMock).decrementOnboardedAndBudget(eq("INIT_1"),eq("USER_1"),eq(1000L));
        verify(transactionInProgressRepositoryMock).deleteById(eq(transactionInProgressDTO.getId()));

    }

    @Test
    public void shouldNotExecuteDecrementAndTrxRemovalIfUserIsNotMapped() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        verifyNoInteractions(initiativeCountersRepositoryMock);
        verifyNoInteractions(transactionInProgressRepositoryMock);
    }

    @Test
    public void shouldNotExecuteDeleteIfErrorOnCounterUpdate() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        when(transactionInProgressRepositoryMock.existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.EXPIRED))).thenReturn(true);
        when(initiativeCountersRepositoryMock.existsByInitiativeIdAndUserId(eq("INIT_1"), eq("USER_1")))
                .thenReturn(true);
        when(initiativeCountersRepositoryMock.decrementOnboardedAndBudget(eq("INIT_1"),eq("USER_1"),eq(1000L)))
                .thenThrow(new RuntimeException("error"));
        Assertions.assertThrows(RuntimeException.class,
                () -> expiredTransactionInProgressProcessorStrategy.processTransaction(transactionInProgressDTO));
        verify(transactionInProgressRepositoryMock).existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.EXPIRED));
        verify(initiativeCountersRepositoryMock).existsByInitiativeIdAndUserId(eq("INIT_1"),eq("USER_1"));
        verify(initiativeCountersRepositoryMock).decrementOnboardedAndBudget(eq("INIT_1"),eq("USER_1"),eq(1000L));
        verifyNoMoreInteractions(transactionInProgressRepositoryMock);
    }

    @Test
    public void shouldThrowExceptionOnDeleteErrorOnCounterUpdate() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        when(transactionInProgressRepositoryMock.existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.EXPIRED))).thenReturn(true);
        when(initiativeCountersRepositoryMock.existsByInitiativeIdAndUserId(eq("INIT_1"), eq("USER_1")))
                .thenReturn(true);
        when(initiativeCountersRepositoryMock.decrementOnboardedAndBudget(eq("INIT_1"),eq("USER_1"),eq(1000L)))
                .thenReturn(new InitiativeCounters());
        doThrow(new RuntimeException("error")).doNothing().when(transactionInProgressRepositoryMock).deleteById(eq("ID_1"));
        Assertions.assertThrows(Exception.class, () -> expiredTransactionInProgressProcessorStrategy.processTransaction(transactionInProgressDTO));
        verify(initiativeCountersRepositoryMock).existsByInitiativeIdAndUserId(eq("INIT_1"),eq("USER_1"));
        verify(initiativeCountersRepositoryMock).decrementOnboardedAndBudget(eq("INIT_1"),eq("USER_1"),eq(1000L));
        verify(transactionInProgressRepositoryMock).deleteById(eq(transactionInProgressDTO.getId()));
    }


}
