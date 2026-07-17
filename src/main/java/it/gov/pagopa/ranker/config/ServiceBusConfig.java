package it.gov.pagopa.ranker.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ServiceBusConfig {
    private final String connectionString;

    public ServiceBusConfig(@Value("${azure.servicebus.onboarding-request.connection-string}") String connectionString) {
        this.connectionString = connectionString;
    }

    @Bean(name = "serviceBusClientBuilderOnboarding")
    public ServiceBusClientBuilder serviceBusClientBuilderOnboarding() {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString);
    }

    @Bean(name = "sessionExecutor")
    public Executor sessionExecutor(RankerProcessorProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getMaxParallelSessions());
        executor.setMaxPoolSize(properties.getMaxParallelSessions());
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("session-worker-");
        executor.initialize();
        return executor;
    }
}