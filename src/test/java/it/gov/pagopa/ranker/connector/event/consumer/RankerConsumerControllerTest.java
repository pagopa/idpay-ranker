package it.gov.pagopa.ranker.connector.event.consumer;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

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
  @SuppressWarnings("java:S2095") // false positive: processorClient is a Mockito mock
  void init_createsProcessorClient() {
    verify(factory, times(1)).create(handler);
  }

  // ---------- isRunning() branches ----------

  @Test
  void isRunning_false_whenProcessorNull() throws Exception {
    setProcessorClient(controller);
    assertFalse(controller.isRunning());
  }

  @Test
  void isRunning_true_whenProcessorRunning() {
    when(processorClient.isRunning()).thenReturn(true);
    assertTrue(controller.isRunning());
  }

  @Test
  void isRunning_false_whenProcessorNotRunning() {
    when(processorClient.isRunning()).thenReturn(false);
    assertFalse(controller.isRunning());
  }

  // ---------- startIfAllowed() branches ----------

  @Test
  void startIfAllowed_returnsImmediately_whenShuttingDown() throws Exception {
    setShuttingDown(controller);

    controller.startIfAllowed();

    verifyNoInteractions(initiativeCountersService);
    verify(processorClient, never()).start();
  }

  @Test
  void startIfAllowed_skipped_whenProcessorNull() throws Exception {
    setProcessorClient(controller);

    controller.startIfAllowed();

    verifyNoInteractions(initiativeCountersService);
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
    verifyNoInteractions(initiativeCountersService);
  }

  @Test
  void startIfAllowed_skipped_whenNoBudget() {
    when(initiativeCountersService.hasAvailableBudget()).thenReturn(false);

    controller.startIfAllowed();

    verify(processorClient, never()).start();
    // budget=false -> non deve arrivare a chiedere isRunning()
    verify(processorClient, never()).isRunning();
  }

  @Test
  void startIfAllowed_doesNotStart_whenAlreadyRunning() {
    when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);
    when(processorClient.isRunning()).thenReturn(true);

    controller.startIfAllowed();

    verify(processorClient, never()).start();
  }

  @Test
  void startIfAllowed_starts_whenBudgetAndNotRunning() {
    when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);
    when(processorClient.isRunning()).thenReturn(false);

    controller.startIfAllowed();

    verify(processorClient, times(1)).start();
  }

  // ---------- lifecycle start/stop branches ----------

  @Test
  void start_lifecycle_delegatesToStartIfAllowed() {
    when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);
    when(processorClient.isRunning()).thenReturn(false);

    controller.start();

    verify(processorClient, times(1)).start();
  }

  @Test
  void stop_shutdown_setsShuttingDown_andCloses_whenProcessorNotNull() throws Exception {

    controller.stop();

    assertTrue(getShuttingDown(controller).get());
    verify(processorClient, times(1)).close();
  }

  @Test
  void stop_shutdown_setsShuttingDown_andDoesNothing_whenProcessorNull() throws Exception {
    setProcessorClient(controller);

    controller.stop();

    assertTrue(getShuttingDown(controller).get());
    // nessuna close possibile
    verify(processorClient, never()).close();
  }

  // ---------- event branch ----------

  @Test
  void onBudgetExhausted_stops_whenRunning() {
    when(processorClient.isRunning()).thenReturn(true);

    controller.onBudgetExhausted();

    verify(processorClient, times(1)).stop();
    verify(processorClient, never()).close();
  }

  @Test
  void onBudgetExhausted_doesNothing_whenNotRunning() {
    when(processorClient.isRunning()).thenReturn(false);

    controller.onBudgetExhausted();

    verify(processorClient, never()).stop();
  }

  // ---------- reflection helpers ----------

  private static void setProcessorClient(RankerConsumerController target)
      throws Exception {
    Field field = RankerConsumerController.class.getDeclaredField("processorClient");
    field.setAccessible(true);
    field.set(target, null);
  }

  private static void setShuttingDown(RankerConsumerController target) throws Exception {
    getShuttingDown(target).set(true);
  }

  private static AtomicBoolean getShuttingDown(RankerConsumerController target) throws Exception {
    Field field = RankerConsumerController.class.getDeclaredField("shuttingDown");
    field.setAccessible(true);
    return (AtomicBoolean) field.get(target);
  }
}