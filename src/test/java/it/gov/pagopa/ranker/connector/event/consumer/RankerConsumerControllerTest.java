package it.gov.pagopa.ranker.connector.event.consumer;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankerConsumerControllerTest {

  @Mock private InitiativeCountersService initiativeCountersService;
  @Mock private ServiceBusProcessorClientFactory factory;
  @Mock private RankerMessageHandler handler;
  @Mock private ServiceBusProcessorClient processorClient;

  private RankerConsumerController controller;

  @BeforeEach
  void setup() {
    controller = new RankerConsumerController(
        initiativeCountersService,
        factory,
        handler,
        false
    );

    when(factory.create(handler)).thenReturn(processorClient);

    controller.init();
  }

  @Test
  @SuppressWarnings("java:S2095")
  void init_createsProcessorClient() {
    verify(factory, times(1)).create(handler);
  }

  @Test
  void isConsumerRunning_false_whenNull() throws Exception {
    setPrivateField(controller);
    org.junit.jupiter.api.Assertions.assertFalse(controller.isConsumerRunning());
  }

  @Test
  void isConsumerRunning_delegatesToProcessor() {
    when(processorClient.isRunning()).thenReturn(true);
    org.junit.jupiter.api.Assertions.assertTrue(controller.isConsumerRunning());
  }

  @Test
  void startIfAllowed_skipped_whenProcessorNull() throws Exception {
    setPrivateField(controller);

    controller.startIfAllowed();

    verify(processorClient, never()).start();
  }

  @Test
  void startIfAllowed_skipped_whenForceStopped() {
    RankerConsumerController forceStoppedController = new RankerConsumerController(
        initiativeCountersService, factory, handler, true
    );
    when(factory.create(handler)).thenReturn(processorClient);
    forceStoppedController.init();

    forceStoppedController.startIfAllowed();

    verify(processorClient, never()).start();
    verify(initiativeCountersService, never()).hasAvailableBudget();
    verify(processorClient, never()).isRunning();
  }

  @Test
  void startIfAllowed_skipped_whenNoBudget() {
    when(initiativeCountersService.hasAvailableBudget()).thenReturn(false);

    controller.startIfAllowed();

    verify(processorClient, never()).start();
    verify(processorClient, never()).isRunning();
  }

  @Test
  void startIfAllowed_starts_whenBudgetAndNotRunning() {
    when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);
    when(processorClient.isRunning()).thenReturn(false);

    controller.startIfAllowed();

    verify(processorClient, times(1)).start();
  }

  @Test
  void startIfAllowed_doesNotStart_whenAlreadyRunning() {
    when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);
    when(processorClient.isRunning()).thenReturn(true);

    controller.startIfAllowed();

    verify(processorClient, never()).start();
  }

  @Test
  void checkResidualBudgetAndStartConsumer_delegatesToStartIfAllowed() {
    when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);
    when(processorClient.isRunning()).thenReturn(false);

    controller.checkResidualBudgetAndStartConsumer();

    verify(processorClient, times(1)).start();
  }

  @Test
  void stopForBudgetExhausted_callsStop_notClose() {
    when(processorClient.isRunning()).thenReturn(true);

    controller.stopForBudgetExhausted();

    verify(processorClient, times(1)).stop();
    verify(processorClient, never()).close();
  }

  @Test
  void stopOnShutdown_callsClose() {
    when(processorClient.isRunning()).thenReturn(true);

    controller.stopOnShutdown();

    verify(processorClient, times(1)).close();
  }

  private static void setPrivateField(Object target) throws Exception {
    Field field = target.getClass().getDeclaredField("processorClient");
    field.setAccessible(true);
    field.set(target, null);
  }
}