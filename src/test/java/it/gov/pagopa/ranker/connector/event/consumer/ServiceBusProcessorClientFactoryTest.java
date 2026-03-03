package it.gov.pagopa.ranker.connector.event.consumer;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ServiceBusProcessorClientFactoryTest {

  @Test
  void create_buildsProcessorClient() {

    ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorBuilder =
        mock(ServiceBusClientBuilder.ServiceBusProcessorClientBuilder.class);

    ServiceBusProcessorClient expectedClient = mock(ServiceBusProcessorClient.class);
    RankerMessageHandler handler = mock(RankerMessageHandler.class);

    try (MockedConstruction<ServiceBusClientBuilder> ignored =
        mockConstruction(ServiceBusClientBuilder.class, (builderMock, context) -> {

          when(builderMock.connectionString(anyString())).thenReturn(builderMock);

          when(builderMock.processor()).thenReturn(processorBuilder);

          when(processorBuilder.queueName(anyString())).thenReturn(processorBuilder);
          when(processorBuilder.processMessage(any())).thenReturn(processorBuilder);
          when(processorBuilder.processError(any())).thenReturn(processorBuilder);
          when(processorBuilder.buildProcessorClient()).thenReturn(expectedClient);
        })) {

      ServiceBusProcessorClientFactory factory =
          new ServiceBusProcessorClientFactory("conn", "queue");

      ServiceBusProcessorClient actual = factory.create(handler);

      assertSame(expectedClient, actual);
    }
  }
}