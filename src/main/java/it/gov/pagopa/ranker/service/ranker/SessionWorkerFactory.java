package it.gov.pagopa.ranker.service.ranker;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import it.gov.pagopa.ranker.config.RankerProcessorProperties;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SessionWorkerFactory {

    private final ServiceBusClientBuilder clientBuilder;
    private final RankerProcessorProperties properties;
    private final InitiativeCountersService initiativeCountersService;
    private final RankerService rankerService;
    private final String queueName;

    public SessionWorkerFactory(@Qualifier("serviceBusClientBuilderOnboarding") ServiceBusClientBuilder clientBuilder,
                                RankerProcessorProperties properties, InitiativeCountersService initiativeCountersService, RankerService rankerService,
                                @Value("${azure.servicebus.onboarding-request.queue-name}") String queueName) {
        this.clientBuilder = clientBuilder;
        this.properties = properties;
        this.initiativeCountersService = initiativeCountersService;
        this.rankerService = rankerService;
        this.queueName = queueName;
    }

    public SessionWorker create(String sessionId, Runnable onCompleted) {
        return new SessionWorker(
                sessionId,
                queueName,
                clientBuilder,
                initiativeCountersService,
                rankerService,
                properties,
                onCompleted
        );
    }
}