package it.gov.pagopa.ranker.strategy.command;

import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.exception.UnmanagedStrategyException;
import it.gov.pagopa.ranker.strategy.ExpiredTransactionInProgressProcessorStrategy;
import it.gov.pagopa.ranker.strategy.TransactionInProgressProcessorStrategy;
import it.gov.pagopa.ranker.strategy.TransactionInProgressProcessorStrategyFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommandProcessorStrategyFactoryTest {

    @Mock
    private CommandProcessorStrategy processorStrategy;

    private CommandProcessorStrategyFactory commandProcessorStrategyFactory;

    @BeforeEach
    public void init() {
        when(processorStrategy.getCommandType()).thenReturn("TEST_COMMAND");
        commandProcessorStrategyFactory = new CommandProcessorStrategyFactory(List.of(processorStrategy));
    }

    @Test
    public void expiredStatusShouldReturnStrategy() {
        CommandProcessorStrategy commandProcessorStrategy =
                Assertions.assertDoesNotThrow(() ->
                        commandProcessorStrategyFactory.getStrategy("TEST_COMMAND"));
        Assertions.assertNotNull(commandProcessorStrategy);
    }

    @Test
    public void unaManagedStatusShouldThrowException() {
        Assertions.assertThrows(UnmanagedStrategyException.class, () ->
                commandProcessorStrategyFactory.getStrategy("UNEXISTING_COMMAND"));
    }

    @Test
    public void nullStatusShouldThrowException() {
        Assertions.assertThrows(AssertionError.class, () ->
                commandProcessorStrategyFactory.getStrategy(null));
    }

}
