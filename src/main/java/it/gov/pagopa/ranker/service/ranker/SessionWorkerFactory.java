package it.gov.pagopa.ranker.service.ranker;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import it.gov.pagopa.ranker.config.RankerProcessorProperties;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.ranker.service.ranker.sessionworker.ManualOnlySessionWorker;
import it.gov.pagopa.ranker.service.ranker.sessionworker.NormalSessionWorker;
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

    public NormalSessionWorker create(String sessionId, Runnable onCompleted) {
        return new NormalSessionWorker(
                sessionId,
                queueName,
                clientBuilder,
                initiativeCountersService,
                rankerService,
                properties,
                onCompleted
        );
    }

    public ManualOnlySessionWorker createManualOnly(
            String sessionId,
            int requestedMessages,
            Runnable onCompleted
    ) {
        return new ManualOnlySessionWorker(
                sessionId,
                queueName,
                clientBuilder,
                initiativeCountersService,
                rankerService,
                properties,
                requestedMessages,
                onCompleted
        );
    }
}