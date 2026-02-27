package it.gov.pagopa.ranker.connector.event.consumer;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
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
    verify(factory).create(handler);
  }

  @Test
  void isRunning_false_whenProcessorNull() throws Exception {
    setProcessorClient(controller);
    assertFalse(controller.isRunning());
  }

  @Test
  void isRunning_delegatesToProcessor() {
    when(processorClient.isRunning()).thenReturn(true);
    assertTrue(controller.isRunning());
  }

  @Test
  void startIfAllowed_skipped_whenProcessorNull() throws Exception {
    setProcessorClient(controller);

    controller.startIfAllowed();

    verify(processorClient, never()).start();
  }

  @Test
  void startIfAllowed_skipped_whenForceStopped() {
    RankerConsumerController forceStoppedController =
        new RankerConsumerController(initiativeCountersService, factory, handler, true);

    when(factory.create(handler)).thenReturn(processorClient);
    forceStoppedController.init();

    forceStoppedController.startIfAllowed();

    verify(processorClient, never()).start();
    verify(initiativeCountersService, never()).hasAvailableBudget();
  }

  @Test
  void startIfAllowed_skipped_whenNoBudget() {
    when(initiativeCountersService.hasAvailableBudget()).thenReturn(false);

    controller.startIfAllowed();

    verify(processorClient, never()).start();
  }

  @Test
  void startIfAllowed_starts_whenBudgetAndNotRunning() {
    when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);
    when(processorClient.isRunning()).thenReturn(false);

    controller.startIfAllowed();

    verify(processorClient).start();
  }

  @Test
  void startIfAllowed_doesNotStart_whenAlreadyRunning() {
    when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);
    when(processorClient.isRunning()).thenReturn(true);

    controller.startIfAllowed();

    verify(processorClient, never()).start();
  }

  @Test
  void lifecycle_start_triggersStartIfAllowed() {
    when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);
    when(processorClient.isRunning()).thenReturn(false);

    controller.start();

    verify(processorClient).start();
  }

  @Test
  void onBudgetExhausted_stopsProcessor() {
    when(processorClient.isRunning()).thenReturn(true);

    controller.onBudgetExhausted();

    verify(processorClient).stop();
    verify(processorClient, never()).close();
  }

  private static void setProcessorClient(RankerConsumerController target)
      throws Exception {
    Field field = RankerConsumerController.class.getDeclaredField("processorClient");
    field.setAccessible(true);
    field.set(target, null);
  }
}