package it.gov.pagopa.ranker.connector.event.consumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServiceBusConsumerLifecycleTest {

  @Test
  void start_callsBudgetCheckAndMarksRunning() {
    RankerConsumerClient client = mock(RankerConsumerClient.class);
    ServiceBusConsumerLifecycle lifecycle = new ServiceBusConsumerLifecycle(client);

    lifecycle.start();

    verify(client, times(1)).checkResidualBudgetAndStartConsumer();
    assertTrue(lifecycle.isRunning());
  }

  @Test
  void stop_callsStopConsumerAndMarksNotRunning() {
    RankerConsumerClient client = mock(RankerConsumerClient.class);
    ServiceBusConsumerLifecycle lifecycle = new ServiceBusConsumerLifecycle(client);

    lifecycle.start();
    lifecycle.stop();

    verify(client, times(1)).stopConsumer();
    assertFalse(lifecycle.isRunning());
  }
}
