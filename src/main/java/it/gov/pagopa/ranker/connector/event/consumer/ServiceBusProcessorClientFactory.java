package it.gov.pagopa.ranker.connector.event.consumer;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServiceBusProcessorClientFactory {

  private final String connectionString;
  private final String queueName;

  public ServiceBusProcessorClientFactory(
      @Value("${azure.servicebus.onboarding-request.connection-string}") String connectionString,
      @Value("${azure.servicebus.onboarding-request.queue-name}") String queueName
  ) {
    this.connectionString = connectionString;
    this.queueName = queueName;
  }

  public ServiceBusProcessorClient create(RankerMessageHandler handler) {
    return new ServiceBusClientBuilder()
        .connectionString(connectionString)
        .processor()
        .queueName(queueName)
        .processMessage(handler::handle)
        .processError(ctx -> log.error("[RANKER_CONTEXT] Error in processor: {}", ctx.getException().getMessage()))
        .buildProcessorClient();
  }
}