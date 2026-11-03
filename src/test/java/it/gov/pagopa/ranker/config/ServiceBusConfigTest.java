package it.gov.pagopa.ranker.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;

class ServiceBusConfigTest {

    private ServiceBusConfig serviceBusConfig;

    @BeforeEach
    void setUp() {
        serviceBusConfig = new ServiceBusConfig("Endpoint=sb://ServiceBusEndpoint;SharedAccessKeyName=sharedAccessKeyName;SharedAccessKey=sharedAccessKey;EntityPath=entityPath");
    }

    @Test
    void serviceBusClientBuilderOnboarding() {
        // Given
        ServiceBusClientBuilder serviceBusClientBuilder = serviceBusConfig.serviceBusClientBuilderOnboarding();

        // Then
        Assertions.assertNotNull(serviceBusClientBuilder);
    }

    @Test
    void sessionExecutor() {
        // When
        RankerProcessorProperties properties = new RankerProcessorProperties();
        properties.setMaxParallelSessions(1);

        // Given
        Executor result = serviceBusConfig.sessionExecutor(properties);

        // Then
        Assertions.assertNotNull(result);
    }

    @Test
    void manualDequeueOrchestratorExecutor() {
        // When
        RankerManualDequeueProperties properties = new RankerManualDequeueProperties();
        properties.setCorePoolSize(1);
        properties.setMaxPoolSize(1);
        properties.setQueueCapacity(0);

        // Given
        Executor result = serviceBusConfig.manualDequeueOrchestratorExecutor(properties);

        // Then
        Assertions.assertNotNull(result);
    }
}