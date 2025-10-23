package it.gov.pagopa.ranker.service.command;

import it.gov.pagopa.ranker.domain.dto.QueueCommandOperationDTO;
import it.gov.pagopa.ranker.strategy.command.CommandProcessorStrategy;
import it.gov.pagopa.ranker.strategy.command.CommandProcessorStrategyFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProcessConsumerServiceImplTest {

    @Mock
    private CommandProcessorStrategy commandProcessorStrategy;

    @Mock
    private CommandProcessorStrategyFactory commandProcessorStrategyFactory;

    private ProcessConsumerServiceImpl processConsumerService;

    @BeforeEach
    public void init() {
        Mockito.reset(commandProcessorStrategyFactory);
        processConsumerService = new ProcessConsumerServiceImpl(commandProcessorStrategyFactory);
    }

    @Test
    public void shouldExecuteProcess() {
        QueueCommandOperationDTO queueCommandOperationDTO = QueueCommandOperationDTO.builder().entityId("ID").operationType("TEST").build();
        when(commandProcessorStrategyFactory.getStrategy("TEST")).thenReturn(commandProcessorStrategy);
        Assertions.assertDoesNotThrow(() ->
                processConsumerService.processCommand(queueCommandOperationDTO));
        verify(commandProcessorStrategyFactory).getStrategy(eq("TEST"));
        verify(commandProcessorStrategy).processCommand(any());
    }

    @Test
    public void shouldExitWithoutExceptionOnMissingCommand() {
        QueueCommandOperationDTO queueCommandOperationDTO = QueueCommandOperationDTO.builder().entityId("ID").operationType("TEST").build();
        when(commandProcessorStrategyFactory.getStrategy("TEST")).thenReturn(null);
        Assertions.assertDoesNotThrow(() ->
                processConsumerService.processCommand(queueCommandOperationDTO));
        verify(commandProcessorStrategyFactory).getStrategy(eq("TEST"));
        verifyNoInteractions(commandProcessorStrategy);
    }

    @Test
    public void shouldExitWithExceptionOnGenericError() {
        QueueCommandOperationDTO queueCommandOperationDTO = QueueCommandOperationDTO.builder().entityId("ID").operationType("TEST").build();
        when(commandProcessorStrategyFactory.getStrategy("TEST")).thenReturn(commandProcessorStrategy);
        doThrow(new RuntimeException("error")).doNothing().when(commandProcessorStrategy).processCommand(any());
        Assertions.assertThrows(RuntimeException.class, () ->
                processConsumerService.processCommand(queueCommandOperationDTO));
        verify(commandProcessorStrategyFactory).getStrategy(eq("TEST"));
        verify(commandProcessorStrategy).processCommand(any());
    }

}
