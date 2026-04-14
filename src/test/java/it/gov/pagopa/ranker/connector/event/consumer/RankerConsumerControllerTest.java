package it.gov.pagopa.ranker.connector.event.consumer;

import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.ranker.service.ranker.CycleOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankerConsumerControllerTest {

  @Mock private InitiativeCountersService initiativeCountersService;
  @Mock private CycleOrchestrator cycleOrchestrator;

  private RankerConsumerController controller;

  @BeforeEach
  void setup() {
    controller = new RankerConsumerController(
        initiativeCountersService,
        false,
        cycleOrchestrator
    );
  }

  @Test
  void startIfAllowed_forcedStopped(){
    controller = new RankerConsumerController(
            initiativeCountersService,
            true,
            cycleOrchestrator
    );

    controller.startIfAllowed();

    verify(initiativeCountersService, never()).hasAvailableBudget();
  }

  @Test
  void startIfAllowed_initiativeWithBudgetNotAvailable(){
    when(initiativeCountersService.hasAvailableBudget()).thenReturn(Boolean.FALSE);
    controller.startIfAllowed();

    verify(initiativeCountersService).hasAvailableBudget();
  }

  @Test
  void startIfAllowed_initiativeWithBudgetAvailable(){
    when(initiativeCountersService.hasAvailableBudget()).thenReturn(Boolean.TRUE);
    doNothing().when(cycleOrchestrator).reconcile();

    controller.startIfAllowed();

    verify(initiativeCountersService).hasAvailableBudget();
  }

}