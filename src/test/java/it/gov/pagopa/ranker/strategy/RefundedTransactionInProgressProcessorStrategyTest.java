package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefundedTransactionInProgressProcessorStrategyTest {

    @Mock
    private TransactionInProgressRepository transactionInProgressRepositoryMock;

    @Mock
    private InitiativeCountersRepository initiativeCountersRepositoryMock;

    private RefundedTransactionInProgressProcessorStrategy refundedTransactionInProgressProcessorStrategy;

    @BeforeEach
    public void init() {
        Mockito.reset(initiativeCountersRepositoryMock, initiativeCountersRepositoryMock);
        refundedTransactionInProgressProcessorStrategy =
                new RefundedTransactionInProgressProcessorStrategy(initiativeCountersRepositoryMock,
                        transactionInProgressRepositoryMock);
    }


    @Test
    public void shouldExecuteUpdateIfValid() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setEffectiveAmountCents(1000L);
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        String preallocationId = InitiativeCountersUtils.computePreallocationId(transactionInProgressDTO);
        when(initiativeCountersRepositoryMock.existsById(eq(preallocationId)))
                .thenReturn(true);
        when(initiativeCountersRepositoryMock.updateCounterForRefunded(eq("INIT_1"),eq(1000L)))
                .thenReturn(new InitiativeCounters());
        when(transactionInProgressRepositoryMock.existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.REFUNDED)))
                .thenReturn(true);
        Assertions.assertDoesNotThrow(() -> refundedTransactionInProgressProcessorStrategy
                .processTransaction(transactionInProgressDTO));
        verify(initiativeCountersRepositoryMock).existsById(eq(preallocationId));
        verify(initiativeCountersRepositoryMock).updateCounterForRefunded(eq("INIT_1"),eq(1000L));

    }

    @Test
    public void shouldNotExecuteUpdateIfUserIsNotMapped() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        verifyNoInteractions(initiativeCountersRepositoryMock);
        verifyNoInteractions(transactionInProgressRepositoryMock);
    }


    @Test
    public void shouldThrowExceptionOnCounterUpdateError() {
        TransactionInProgressDTO transactionInProgressDTO = new TransactionInProgressDTO();
        transactionInProgressDTO.setId("ID_1");
        transactionInProgressDTO.setInitiativeId("INIT_1");
        transactionInProgressDTO.setEffectiveAmountCents(1000L);
        transactionInProgressDTO.setVoucherAmountCents(1000L);
        transactionInProgressDTO.setUserId("USER_1");
        String preallocationId = InitiativeCountersUtils.computePreallocationId(transactionInProgressDTO);
        when(initiativeCountersRepositoryMock.existsById(eq(preallocationId)))
                .thenReturn(true);
        when(transactionInProgressRepositoryMock.existsByIdAndStatus(eq("ID_1"),eq(SyncTrxStatus.REFUNDED)))
                .thenReturn(true);
        when(initiativeCountersRepositoryMock.updateCounterForRefunded(eq("INIT_1"),eq(1000L)))
                .thenThrow(new RuntimeException("test"));
        Assertions.assertThrows(Exception.class, () ->
                refundedTransactionInProgressProcessorStrategy.processTransaction(transactionInProgressDTO));
        verify(initiativeCountersRepositoryMock).updateCounterForRefunded(eq("INIT_1"),eq(1000L));
    }


}
