package it.gov.pagopa.ranker.strategy.command;

import it.gov.pagopa.ranker.domain.dto.QueueCommandOperationDTO;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static it.gov.pagopa.ranker.constants.CommonConstants.SEQUENCE_NUMBER_PROPERTY;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteSequenceNumberCommandStrategyTest {

    @Mock
    private InitiativeCountersService initiativeCountersService;

    private DeleteSequenceNumberCommandStrategy addSequenceNumberCommandStrategy;

    @BeforeEach
    public void init() {
        Mockito.reset(initiativeCountersService);
        addSequenceNumberCommandStrategy = new DeleteSequenceNumberCommandStrategy(initiativeCountersService);
    }

    @Test
    public void shouldAddSequenceNumberOnValidRequest() {
        doNothing().when(initiativeCountersService).removeMessageToProcessOnInitative(eq("ENTITY_ID"),eq(1L));
        addSequenceNumberCommandStrategy.processCommand(QueueCommandOperationDTO.builder()
               .entityId("ENTITY_ID")
               .operationTime(LocalDateTime.now())
               .properties(Map.of(SEQUENCE_NUMBER_PROPERTY,"1"))
               .build()
        );
        verify(initiativeCountersService).removeMessageToProcessOnInitative(eq("ENTITY_ID"),eq(1L));
    }

    @Test
    public void shouldNotAddSequenceNumberMissingEntityId() {
        addSequenceNumberCommandStrategy.processCommand(QueueCommandOperationDTO.builder()
                .operationTime(LocalDateTime.now())
                .properties(Map.of(SEQUENCE_NUMBER_PROPERTY,"1"))
                .build()
        );
        verifyNoInteractions(initiativeCountersService);
    }

    @Test
    public void shouldNotAddSequenceNumberMissingProperty() {
        addSequenceNumberCommandStrategy.processCommand(QueueCommandOperationDTO.builder()
                .entityId("ENTITY_ID")
                .operationTime(LocalDateTime.now())
                .build()
        );
        verifyNoInteractions(initiativeCountersService);
    }

    @Test
    public void shouldNotAddSequenceOnWrongProperty() {
        addSequenceNumberCommandStrategy.processCommand(QueueCommandOperationDTO.builder()
                .entityId("ENTITY_ID")
                .operationTime(LocalDateTime.now())
                .properties(Map.of(SEQUENCE_NUMBER_PROPERTY,"1AAA"))
                .build()
        );
        verifyNoInteractions(initiativeCountersService);
    }

    @Test
    public void shouldReturnCommandType() {
        Assertions.assertEquals(
                "DELETE_SEQUENCE_NUMBER",addSequenceNumberCommandStrategy.getCommandType());
    }

}
