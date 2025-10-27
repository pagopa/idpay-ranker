package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.repository.TransactionInProgressRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
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
    private TransactionInProgressRepository transactionInProgressRepositoryMock;

    @Mock
    private InitiativeCountersService initiativeCountersServiceMock;

    private ExpiredTransactionInProgressProcessorStrategy expiredTransactionInProgressProcessorStrategy;

    @BeforeEach
    public void init() {
        expiredTransactionInProgressProcessorStrategy = new ExpiredTransactionInProgressProcessorStrategy(
                transactionInProgressRepositoryMock, initiativeCountersServiceMock);
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
        when(transactionInProgressRepositoryMock.existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.EXPIRED)))
                .thenReturn(true);
        Assertions.assertDoesNotThrow(() -> expiredTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));
        verify(initiativeCountersServiceMock).updateInitiativeCounters(any(),any(),any());
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
        when(transactionInProgressRepositoryMock.existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.EXPIRED)))
                .thenReturn(true);
        doNothing().when(initiativeCountersServiceMock).updateInitiativeCounters(transactionInProgressDTO, preallocationId, transactionInProgressDTO.getId());

        Assertions.assertDoesNotThrow(() -> expiredTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));
        verify(transactionInProgressRepositoryMock).existsByIdAndStatus(any(),any());
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
        when(transactionInProgressRepositoryMock.existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.EXPIRED)))
                .thenReturn(true);
        doThrow(new RuntimeException("error")).when(initiativeCountersServiceMock)
                .updateInitiativeCounters(transactionInProgressDTO, preallocationId, transactionInProgressDTO.getId());

        Assertions.assertThrows(RuntimeException.class,
                () -> expiredTransactionInProgressProcessorStrategy.processTransaction(transactionInProgressDTO));
        verify(transactionInProgressRepositoryMock).existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.EXPIRED));
        verify(initiativeCountersServiceMock).updateInitiativeCounters(any(),any(),any());
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
        doNothing().when(initiativeCountersServiceMock).updateInitiativeCounters(transactionInProgressDTO, preallocationId, transactionInProgressDTO.getId());

        doThrow(new RuntimeException("error")).doNothing().when(transactionInProgressRepositoryMock)
                .deleteById(eq("ID_1"));
        Assertions.assertThrows(Exception.class, () ->
                expiredTransactionInProgressProcessorStrategy.processTransaction(transactionInProgressDTO));
        verify(initiativeCountersServiceMock).updateInitiativeCounters(any(),any(),any());
        verify(transactionInProgressRepositoryMock).deleteById(eq(transactionInProgressDTO.getId()));
    }


}
