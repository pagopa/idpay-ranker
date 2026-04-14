package it.gov.pagopa.ranker.service.ranker;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import it.gov.pagopa.ranker.config.RankerProcessorProperties;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SessionWorkerFactoryTest {
    @Mock private ServiceBusClientBuilder clientBuilderMock;
    @Mock private RankerProcessorProperties propertiesMock;
    @Mock private InitiativeCountersService initiativeCountersServiceMock;
    @Mock private RankerService rankerServiceMock;
    private final String queueName = "QUEUE_NAME";

    private SessionWorkerFactory factory;

    @BeforeEach
    void setUp() {
        factory = new SessionWorkerFactory(clientBuilderMock, propertiesMock, initiativeCountersServiceMock, rankerServiceMock, queueName);
    }

    @Test
    void create() {
        Runnable runnableMock = mock(Runnable.class);
        SessionWorker result = factory.create("INITIATIVE_ID", runnableMock);

        assertNotNull(result);

    }
}