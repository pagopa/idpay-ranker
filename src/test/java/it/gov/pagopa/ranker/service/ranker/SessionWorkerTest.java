package it.gov.pagopa.ranker.service.ranker;

import com.azure.core.amqp.exception.AmqpErrorContext;
import com.azure.core.amqp.exception.AmqpException;
import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import it.gov.pagopa.ranker.config.RankerProcessorProperties;
import it.gov.pagopa.ranker.exception.BudgetExhaustedException;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionWorkerTest {
    @Mock private ServiceBusClientBuilder serviceBusClientBuilderMock;
    @Mock private InitiativeCountersService initiativeCountersServiceMock;
    @Mock private RankerService rankerServiceMock;
    @Mock private Runnable onCompletedMock;
    @Mock private RankerProcessorProperties rankerProcessorPropertiesMock;
    private static final String SESSION_ID = "SESSION_ID";
    private static final String QUEUE_NAME = "QUEUE_NAME";
    private SessionWorker sessionWorker;

    @BeforeEach
    void setUp() {
        sessionWorker = new SessionWorker(SESSION_ID, QUEUE_NAME,serviceBusClientBuilderMock,
                initiativeCountersServiceMock,rankerServiceMock,rankerProcessorPropertiesMock, onCompletedMock);
    }

    @Test
    void run_errorSessionClient(){
        doThrow(new RuntimeException("DUMMY")).when(serviceBusClientBuilderMock).sessionReceiver();
        doNothing().when(onCompletedMock).run();

        sessionWorker.run();
    }

    @Test
    void run_genericErrorLockSession(){

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMock = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(serviceBusClientBuilderMock.sessionReceiver()).thenReturn(clientReceiverBuilderMock);

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMockWithQueue = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(clientReceiverBuilderMock.queueName(QUEUE_NAME)).thenReturn(clientReceiverBuilderMockWithQueue);

        ServiceBusSessionReceiverClient clientBuilderMock = mock(ServiceBusSessionReceiverClient.class);
        when(clientReceiverBuilderMockWithQueue.buildClient()).thenReturn(clientBuilderMock);

        doThrow(new RuntimeException("DUMMY")).when(clientBuilderMock).acceptSession(SESSION_ID);
        doNothing().when(onCompletedMock).run();

        sessionWorker.run();
    }

    @Test
    void run_deniedLockSession(){

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMock = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(serviceBusClientBuilderMock.sessionReceiver()).thenReturn(clientReceiverBuilderMock);

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMockWithQueue = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(clientReceiverBuilderMock.queueName(QUEUE_NAME)).thenReturn(clientReceiverBuilderMockWithQueue);

        ServiceBusSessionReceiverClient clientBuilderMock = mock(ServiceBusSessionReceiverClient.class);
        when(clientReceiverBuilderMockWithQueue.buildClient()).thenReturn(clientBuilderMock);

        AmqpErrorContext errorContext = new AmqpErrorContext("TEST");
        doThrow(new AmqpException(true, "DUMMY",errorContext)).when(clientBuilderMock).acceptSession(SESSION_ID);
        doNothing().when(onCompletedMock).run();

        sessionWorker.run();
    }

    @Test
    void run_checkBudgetAfterLockFalse(){

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMock = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(serviceBusClientBuilderMock.sessionReceiver()).thenReturn(clientReceiverBuilderMock);

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMockWithQueue = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(clientReceiverBuilderMock.queueName(QUEUE_NAME)).thenReturn(clientReceiverBuilderMockWithQueue);

        ServiceBusSessionReceiverClient clientBuilderMock = mock(ServiceBusSessionReceiverClient.class);
        when(clientReceiverBuilderMockWithQueue.buildClient()).thenReturn(clientBuilderMock);

        ServiceBusReceiverClient clientMock = mock(ServiceBusReceiverClient.class);
        when(clientBuilderMock.acceptSession(SESSION_ID)).thenReturn(clientMock);

        when(initiativeCountersServiceMock.hasAvailableBudget(SESSION_ID)).thenReturn(false);
        doNothing().when(onCompletedMock).run();

        sessionWorker.run();
    }

    @Test
    void run_timeout(){

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMock = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(serviceBusClientBuilderMock.sessionReceiver()).thenReturn(clientReceiverBuilderMock);

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMockWithQueue = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(clientReceiverBuilderMock.queueName(QUEUE_NAME)).thenReturn(clientReceiverBuilderMockWithQueue);

        ServiceBusSessionReceiverClient clientBuilderMock = mock(ServiceBusSessionReceiverClient.class);
        when(clientReceiverBuilderMockWithQueue.buildClient()).thenReturn(clientBuilderMock);

        ServiceBusReceiverClient clientMock = mock(ServiceBusReceiverClient.class);
        when(clientBuilderMock.acceptSession(SESSION_ID)).thenReturn(clientMock);

        when(initiativeCountersServiceMock.hasAvailableBudget(SESSION_ID)).thenReturn(true);

        when(rankerProcessorPropertiesMock.getWaitMessageSeconds()).thenReturn(1);
        IterableStream<ServiceBusReceivedMessage> messagesMock = IterableStream.of(Collections.emptyList());
        when(clientMock.receiveMessages(1, Duration.ofSeconds(1))).thenReturn(messagesMock);

        when(rankerProcessorPropertiesMock.getIdleTimeoutSeconds()).thenReturn(1);

        doNothing().when(onCompletedMock).run();

        sessionWorker.run();
    }

    @Test
    void run_budgetExhausted(){

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMock = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(serviceBusClientBuilderMock.sessionReceiver()).thenReturn(clientReceiverBuilderMock);

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMockWithQueue = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(clientReceiverBuilderMock.queueName(QUEUE_NAME)).thenReturn(clientReceiverBuilderMockWithQueue);

        ServiceBusSessionReceiverClient clientBuilderMock = mock(ServiceBusSessionReceiverClient.class);
        when(clientReceiverBuilderMockWithQueue.buildClient()).thenReturn(clientBuilderMock);

        ServiceBusReceiverClient clientMock = mock(ServiceBusReceiverClient.class);
        when(clientBuilderMock.acceptSession(SESSION_ID)).thenReturn(clientMock);

        when(initiativeCountersServiceMock.hasAvailableBudget(SESSION_ID)).thenReturn(true);

        when(rankerProcessorPropertiesMock.getWaitMessageSeconds()).thenReturn(1);
        ServiceBusReceivedMessage messageMock = mock(ServiceBusReceivedMessage.class);
        IterableStream<ServiceBusReceivedMessage> messagesMock = IterableStream.of(Collections.singleton(messageMock));
        when(clientMock.receiveMessages(1, Duration.ofSeconds(1))).thenReturn(messagesMock);

        doThrow(new BudgetExhaustedException("TEST")).when(rankerServiceMock).execute(messageMock);
        doNothing().when(onCompletedMock).run();

        sessionWorker.run();
    }

    @Test
    void run_ok(){

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMock = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(serviceBusClientBuilderMock.sessionReceiver()).thenReturn(clientReceiverBuilderMock);

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMockWithQueue = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(clientReceiverBuilderMock.queueName(QUEUE_NAME)).thenReturn(clientReceiverBuilderMockWithQueue);

        ServiceBusSessionReceiverClient clientBuilderMock = mock(ServiceBusSessionReceiverClient.class);
        when(clientReceiverBuilderMockWithQueue.buildClient()).thenReturn(clientBuilderMock);

        ServiceBusReceiverClient clientMock = mock(ServiceBusReceiverClient.class);
        when(clientBuilderMock.acceptSession(SESSION_ID)).thenReturn(clientMock);

        when(initiativeCountersServiceMock.hasAvailableBudget(SESSION_ID)).thenReturn(true);

        when(rankerProcessorPropertiesMock.getWaitMessageSeconds()).thenReturn(1);
        ServiceBusReceivedMessage messageMock = mock(ServiceBusReceivedMessage.class);
        IterableStream<ServiceBusReceivedMessage> messagesMock = IterableStream.of(Collections.singleton(messageMock));
        IterableStream<ServiceBusReceivedMessage> messagesEmptyMock = IterableStream.of(Collections.emptyList());
        when(clientMock.receiveMessages(1, Duration.ofSeconds(1))).thenReturn(messagesMock).thenReturn(messagesEmptyMock);

        when(rankerProcessorPropertiesMock.getIdleTimeoutSeconds()).thenReturn(5);

        doNothing().when(rankerServiceMock).execute(messageMock);
        doNothing().when(clientMock).complete(messageMock);
        doNothing().when(onCompletedMock).run();

        sessionWorker.run();
    }


}