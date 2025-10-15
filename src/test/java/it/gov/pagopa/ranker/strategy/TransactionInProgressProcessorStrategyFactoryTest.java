package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.exception.UnmanagedStrategyException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionInProgressProcessorStrategyFactoryTest {

    @Mock
    private ExpiredTransactionInProgressProcessorStrategy expiredTransactionInProgressProcessorStrategy;

    private TransactionInProgressProcessorStrategyFactory transactionInProgressProcessorStrategyFactory;

    @BeforeEach
    public void init() {
        when(expiredTransactionInProgressProcessorStrategy.getProcessedStatus()).thenReturn(SyncTrxStatus.EXPIRED);
        transactionInProgressProcessorStrategyFactory = new TransactionInProgressProcessorStrategyFactory(List.of(expiredTransactionInProgressProcessorStrategy));
    }

    @Test
    public void expiredStatusShouldReturnStrategy() {
        TransactionInProgressProcessorStrategy transactionInProgressProcessorStrategy =
                Assertions.assertDoesNotThrow(() ->
                        transactionInProgressProcessorStrategyFactory.getStrategy(SyncTrxStatus.EXPIRED));
        Assertions.assertNotNull(transactionInProgressProcessorStrategy);
        Assertions.assertInstanceOf(ExpiredTransactionInProgressProcessorStrategy.class, transactionInProgressProcessorStrategy);
    }

    @Test
    public void unaManagedStatusShouldThrowException() {
        Assertions.assertThrows(UnmanagedStrategyException.class, () ->
                transactionInProgressProcessorStrategyFactory.getStrategy(SyncTrxStatus.IDENTIFIED));
    }

    @Test
    public void nullStatusShouldThrowException() {
        Assertions.assertThrows(AssertionError.class, () ->
                transactionInProgressProcessorStrategyFactory.getStrategy(null));
    }

}
