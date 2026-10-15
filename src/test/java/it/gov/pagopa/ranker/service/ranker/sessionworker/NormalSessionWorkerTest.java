package it.gov.pagopa.ranker.service.ranker.sessionworker;

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
import it.gov.pagopa.ranker.service.ranker.RankerService;
import it.gov.pagopa.ranker.service.ranker.manualdequeue.ManualDequeueRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NormalSessionWorkerTest {
    @Mock private ServiceBusClientBuilder serviceBusClientBuilderMock;
    @Mock private InitiativeCountersService initiativeCountersServiceMock;
    @Mock private RankerService rankerServiceMock;
    @Mock private Runnable onCompletedMock;
    @Mock private RankerProcessorProperties rankerProcessorPropertiesMock;
    private static final String SESSION_ID = "SESSION_ID";
    private static final String QUEUE_NAME = "QUEUE_NAME";
    private NormalSessionWorker sessionWorker;

    @BeforeEach
    void setUp() {
        sessionWorker = new NormalSessionWorker(SESSION_ID,  QUEUE_NAME,serviceBusClientBuilderMock,
                initiativeCountersServiceMock,rankerServiceMock,rankerProcessorPropertiesMock, onCompletedMock);
    }

    @Test
    void run_errorSessionClient(){
        doThrow(new RuntimeException("DUMMY")).when(serviceBusClientBuilderMock).sessionReceiver();
        doNothing().when(onCompletedMock).run();

        Assertions.assertDoesNotThrow(() -> sessionWorker.run());


    }

    @Test
    void run_genericErrorLockSession(){

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMock = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(serviceBusClientBuilderMock.sessionReceiver()).thenReturn(clientReceiverBuilderMock);

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMockWithQueue = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(clientReceiverBuilderMock.queueName(QUEUE_NAME)).thenReturn(clientReceiverBuilderMockWithQueue);
        lenient().when(clientReceiverBuilderMockWithQueue.disableAutoComplete()).thenReturn(clientReceiverBuilderMockWithQueue);

        ServiceBusSessionReceiverClient clientBuilderMock = mock(ServiceBusSessionReceiverClient.class);
        when(clientReceiverBuilderMockWithQueue.buildClient()).thenReturn(clientBuilderMock);

        doThrow(new RuntimeException("DUMMY")).when(clientBuilderMock).acceptSession(SESSION_ID);
        doNothing().when(onCompletedMock).run();

        sessionWorker.run();

        verifyNoMoreInteractions(serviceBusClientBuilderMock, initiativeCountersServiceMock,rankerServiceMock,onCompletedMock,rankerProcessorPropertiesMock);
    }

    @Test
    void run_deniedLockSession(){

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMock = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(serviceBusClientBuilderMock.sessionReceiver()).thenReturn(clientReceiverBuilderMock);

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMockWithQueue = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(clientReceiverBuilderMock.queueName(QUEUE_NAME)).thenReturn(clientReceiverBuilderMockWithQueue);
        lenient().when(clientReceiverBuilderMockWithQueue.disableAutoComplete()).thenReturn(clientReceiverBuilderMockWithQueue);

        ServiceBusSessionReceiverClient clientBuilderMock = mock(ServiceBusSessionReceiverClient.class);
        when(clientReceiverBuilderMockWithQueue.buildClient()).thenReturn(clientBuilderMock);

        AmqpErrorContext errorContext = new AmqpErrorContext("TEST");
        doThrow(new AmqpException(true, "DUMMY",errorContext)).when(clientBuilderMock).acceptSession(SESSION_ID);
        doNothing().when(onCompletedMock).run();

        sessionWorker.run();

        verifyNoMoreInteractions(serviceBusClientBuilderMock, initiativeCountersServiceMock,rankerServiceMock,onCompletedMock,rankerProcessorPropertiesMock);

    }

    @Test
    void run_checkBudgetAfterLockFalse(){

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMock = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(serviceBusClientBuilderMock.sessionReceiver()).thenReturn(clientReceiverBuilderMock);

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMockWithQueue = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(clientReceiverBuilderMock.queueName(QUEUE_NAME)).thenReturn(clientReceiverBuilderMockWithQueue);
        lenient().when(clientReceiverBuilderMockWithQueue.disableAutoComplete()).thenReturn(clientReceiverBuilderMockWithQueue);

        ServiceBusSessionReceiverClient clientBuilderMock = mock(ServiceBusSessionReceiverClient.class);
        when(clientReceiverBuilderMockWithQueue.buildClient()).thenReturn(clientBuilderMock);

        ServiceBusReceiverClient clientMock = mock(ServiceBusReceiverClient.class);
        when(clientBuilderMock.acceptSession(SESSION_ID)).thenReturn(clientMock);

        when(initiativeCountersServiceMock.hasAvailableBudget(SESSION_ID)).thenReturn(false);
        doNothing().when(onCompletedMock).run();

        sessionWorker.run();

        verifyNoMoreInteractions(serviceBusClientBuilderMock, initiativeCountersServiceMock,rankerServiceMock,onCompletedMock,rankerProcessorPropertiesMock);

    }

    @Test
    void run_timeout(){

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMock = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(serviceBusClientBuilderMock.sessionReceiver()).thenReturn(clientReceiverBuilderMock);

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMockWithQueue = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(clientReceiverBuilderMock.queueName(QUEUE_NAME)).thenReturn(clientReceiverBuilderMockWithQueue);
        lenient().when(clientReceiverBuilderMockWithQueue.disableAutoComplete()).thenReturn(clientReceiverBuilderMockWithQueue);

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

        verifyNoMoreInteractions(serviceBusClientBuilderMock, initiativeCountersServiceMock,rankerServiceMock,onCompletedMock,rankerProcessorPropertiesMock);

    }

    @Test
    void run_budgetExhausted(){

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMock = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(serviceBusClientBuilderMock.sessionReceiver()).thenReturn(clientReceiverBuilderMock);

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMockWithQueue = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(clientReceiverBuilderMock.queueName(QUEUE_NAME)).thenReturn(clientReceiverBuilderMockWithQueue);
        lenient().when(clientReceiverBuilderMockWithQueue.disableAutoComplete()).thenReturn(clientReceiverBuilderMockWithQueue);

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

        verifyNoMoreInteractions(serviceBusClientBuilderMock, initiativeCountersServiceMock,rankerServiceMock,onCompletedMock,rankerProcessorPropertiesMock);

    }

    @Test
    void run_ok(){

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMock = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(serviceBusClientBuilderMock.sessionReceiver()).thenReturn(clientReceiverBuilderMock);

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder clientReceiverBuilderMockWithQueue = mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);
        when(clientReceiverBuilderMock.queueName(QUEUE_NAME)).thenReturn(clientReceiverBuilderMockWithQueue);
        lenient().when(clientReceiverBuilderMockWithQueue.disableAutoComplete()).thenReturn(clientReceiverBuilderMockWithQueue);

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

        verifyNoMoreInteractions(serviceBusClientBuilderMock, initiativeCountersServiceMock,rankerServiceMock,onCompletedMock,rankerProcessorPropertiesMock);

    }

    @Test
    void tryEnqueueManualRequest_firstRequestAccepted_secondRequestRejected() {
        Runnable firstOnCompletedMock = mock(Runnable.class);
        Runnable secondOnCompletedMock = mock(Runnable.class);

        ManualDequeueRequest firstRequest = new ManualDequeueRequest(
                SESSION_ID,
                1,
                "TRACE_ID_1",
                firstOnCompletedMock
        );

        ManualDequeueRequest secondRequest = new ManualDequeueRequest(
                SESSION_ID,
                1,
                "TRACE_ID_2",
                secondOnCompletedMock
        );

        Assertions.assertTrue(
                sessionWorker.tryEnqueueManualRequest(firstRequest)
        );

        Assertions.assertFalse(
                sessionWorker.tryEnqueueManualRequest(secondRequest)
        );

        verifyNoInteractions(firstOnCompletedMock, secondOnCompletedMock);
    }

    @Test
    void run_withPendingManualRequest_processesRequestAndReleasesOwnership() {
        ServiceBusReceiverClient clientMock = configureAcceptedSession();

        Runnable manualRequestOnCompletedMock = mock(Runnable.class);

        ManualDequeueRequest manualRequest = new ManualDequeueRequest(
                SESSION_ID,
                1,
                "MANUAL_TRACE_ID",
                manualRequestOnCompletedMock
        );

        Assertions.assertTrue(
                sessionWorker.tryEnqueueManualRequest(manualRequest)
        );

        ServiceBusReceivedMessage manualMessageMock =
                mock(ServiceBusReceivedMessage.class);

        when(initiativeCountersServiceMock.hasAvailableBudget(SESSION_ID))
                .thenReturn(true);

        when(rankerProcessorPropertiesMock.getWaitMessageSeconds())
                .thenReturn(1);

        when(rankerProcessorPropertiesMock.getIdleTimeoutSeconds())
                .thenReturn(1);

        IterableStream<ServiceBusReceivedMessage> manualMessageStream =
                IterableStream.of(Collections.singleton(manualMessageMock));

        IterableStream<ServiceBusReceivedMessage> emptyMessageStream =
                IterableStream.of(Collections.emptyList());

        /*
         * First receiveMessages call:
         * manual request processing.
         *
         * Second receiveMessages call:
         * normal processing iteration, which receives no message and reaches
         * the configured idle timeout.
         */
        when(clientMock.receiveMessages(1, Duration.ofSeconds(1)))
                .thenReturn(manualMessageStream)
                .thenReturn(emptyMessageStream);

        sessionWorker.run();

        verify(rankerServiceMock).execute(manualMessageMock);
        verify(clientMock).complete(manualMessageMock);

        /*
         * Callback associated with the manual-dequeue ownership.
         */
        verify(manualRequestOnCompletedMock).run();

        /*
         * Callback associated with the whole SessionWorker lifecycle.
         */
        verify(onCompletedMock).run();

        /*
         * The worker has stopped, therefore it must reject future requests.
         */
        Assertions.assertFalse(
                sessionWorker.tryEnqueueManualRequest(
                        new ManualDequeueRequest(
                                SESSION_ID,
                                1,
                                "ANOTHER_TRACE_ID",
                                mock(Runnable.class)
                        )
                )
        );
    }

    @Test
    void run_withPendingManualRequestReceiveError_releasesOwnershipAndStopsWorker() {
        Runnable manualRequestOnCompletedMock;
        try (ServiceBusReceiverClient clientMock = configureAcceptedSession()) {

            manualRequestOnCompletedMock = mock(Runnable.class);

            ManualDequeueRequest manualRequest = new ManualDequeueRequest(
                    SESSION_ID,
                    1,
                    "MANUAL_TRACE_ID",
                    manualRequestOnCompletedMock
            );

            Assertions.assertTrue(
                    sessionWorker.tryEnqueueManualRequest(manualRequest)
            );

            when(initiativeCountersServiceMock.hasAvailableBudget(SESSION_ID))
                    .thenReturn(true);

            when(rankerProcessorPropertiesMock.getWaitMessageSeconds())
                    .thenReturn(1);

            when(
                    clientMock.receiveMessages(
                            1,
                            Duration.ofSeconds(1)
                    )
            ).thenThrow(new RuntimeException("Service Bus receive error"));
        }

        Assertions.assertDoesNotThrow(
                () -> sessionWorker.run()
        );

        /*
         * executeManualRequest(...).finally must release the ownership.
         */
        verify(manualRequestOnCompletedMock).run();

        /*
         * The full SessionWorker lifecycle completion callback.
         */
        verify(onCompletedMock).run();

        verifyNoInteractions(rankerServiceMock);

        /*
         * The catch in executeManualRequest(...) called requestStop(),
         * therefore the worker must reject a new manual request.
         */
        Assertions.assertFalse(
                sessionWorker.tryEnqueueManualRequest(
                        new ManualDequeueRequest(
                                SESSION_ID,
                                1,
                                "ANOTHER_TRACE_ID",
                                mock(Runnable.class)
                        )
                )
        );
    }


    @Test
    void onWorkerStopping_withPendingManualRequest_releasesRequestAndRejectsNewRequests() {
        Runnable manualRequestOnCompletedMock = mock(Runnable.class);

        ManualDequeueRequest manualRequest = new ManualDequeueRequest(
                SESSION_ID,
                1,
                "MANUAL_TRACE_ID",
                manualRequestOnCompletedMock
        );

        Assertions.assertTrue(
                sessionWorker.tryEnqueueManualRequest(manualRequest)
        );

        /*
         * Simulates shutdown before the worker loop claims the pending request.
         */
        sessionWorker.onWorkerStopping();

        verify(manualRequestOnCompletedMock).run();

        Assertions.assertFalse(
                sessionWorker.tryEnqueueManualRequest(
                        new ManualDequeueRequest(
                                SESSION_ID,
                                1,
                                "ANOTHER_TRACE_ID",
                                mock(Runnable.class)
                        )
                )
        );
    }

    @Test
    void onWorkerStopping_whenManualRequestReleaseFails_doesNotThrow() {
        Runnable manualRequestOnCompletedMock = mock(Runnable.class);

        ManualDequeueRequest manualRequest = new ManualDequeueRequest(
                SESSION_ID,
                1,
                "MANUAL_TRACE_ID",
                manualRequestOnCompletedMock
        );

        Assertions.assertTrue(
                sessionWorker.tryEnqueueManualRequest(manualRequest)
        );

        doThrow(new RuntimeException("Release callback error"))
                .when(manualRequestOnCompletedMock)
                .run();

        Assertions.assertDoesNotThrow(
                () -> sessionWorker.onWorkerStopping()
        );

        verify(manualRequestOnCompletedMock).run();
    }

    private ServiceBusReceiverClient configureAcceptedSession() {
        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder
                clientReceiverBuilderMock =
                mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);

        when(serviceBusClientBuilderMock.sessionReceiver())
                .thenReturn(clientReceiverBuilderMock);

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder
                clientReceiverBuilderMockWithQueue =
                mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);

        when(clientReceiverBuilderMock.queueName(QUEUE_NAME))
                .thenReturn(clientReceiverBuilderMockWithQueue);

        lenient().when(clientReceiverBuilderMockWithQueue.disableAutoComplete())
                .thenReturn(clientReceiverBuilderMockWithQueue);

        ServiceBusSessionReceiverClient clientBuilderMock =
                mock(ServiceBusSessionReceiverClient.class);

        when(clientReceiverBuilderMockWithQueue.buildClient())
                .thenReturn(clientBuilderMock);

        ServiceBusReceiverClient clientMock =
                mock(ServiceBusReceiverClient.class);

        when(clientBuilderMock.acceptSession(SESSION_ID))
                .thenReturn(clientMock);

        return clientMock;
    }

}