package it.gov.pagopa.ranker.service.ranker.sessionworker;

import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import it.gov.pagopa.ranker.config.RankerProcessorProperties;
import it.gov.pagopa.ranker.exception.ManualDequeueSizeNotValidException;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualOnlySessionWorkerTest {

    private static final String SESSION_ID = "SESSION_ID";
    private static final String QUEUE_NAME = "QUEUE_NAME";
    private static final int REQUESTED_MESSAGES = 2;
    private static final int WAIT_MESSAGE_SECONDS = 1;

    @Mock
    private ServiceBusClientBuilder serviceBusClientBuilderMock;

    @Mock
    private InitiativeCountersService initiativeCountersServiceMock;

    @Mock
    private RankerService rankerServiceMock;

    @Mock
    private RankerProcessorProperties rankerProcessorPropertiesMock;

    private ManualOnlySessionWorker sessionWorker;

    @BeforeEach
    void setUp() {
        sessionWorker = new ManualOnlySessionWorker(
                SESSION_ID,
                QUEUE_NAME,
                serviceBusClientBuilderMock,
                initiativeCountersServiceMock,
                rankerServiceMock,
                rankerProcessorPropertiesMock,
                REQUESTED_MESSAGES
        );
    }

    @Test
    void constructor_withNonPositiveRequestedMessages_throwsException() {
        Assertions.assertThrows(
                ManualDequeueSizeNotValidException.class,
                () -> new ManualOnlySessionWorker(
                        SESSION_ID,
                        QUEUE_NAME,
                        serviceBusClientBuilderMock,
                        initiativeCountersServiceMock,
                        rankerServiceMock,
                        rankerProcessorPropertiesMock,
                        0
                )
        );

        verifyNoInteractions(
                serviceBusClientBuilderMock,
                initiativeCountersServiceMock,
                rankerServiceMock,
                rankerProcessorPropertiesMock
        );
    }

    @Test
    void run_ok_processesRequestedMessagesAndCompletesWorker() {
        ServiceBusReceiverClient receiverMock = configureSessionReceiver();

        ServiceBusReceivedMessage firstMessageMock =
                mock(ServiceBusReceivedMessage.class);

        ServiceBusReceivedMessage secondMessageMock =
                mock(ServiceBusReceivedMessage.class);

        when(initiativeCountersServiceMock.hasAvailableBudget(SESSION_ID))
                .thenReturn(true);

        when(rankerProcessorPropertiesMock.getWaitMessageSeconds())
                .thenReturn(WAIT_MESSAGE_SECONDS);

        IterableStream<ServiceBusReceivedMessage> firstBatch =
                IterableStream.of(Collections.singletonList(firstMessageMock));

        IterableStream<ServiceBusReceivedMessage> secondBatch =
                IterableStream.of(Collections.singletonList(secondMessageMock));

        when(
                receiverMock.receiveMessages(
                        1,
                        Duration.ofSeconds(WAIT_MESSAGE_SECONDS)
                )
        ).thenReturn(firstBatch, secondBatch);

        doNothing().when(rankerServiceMock).execute(firstMessageMock);
        doNothing().when(rankerServiceMock).execute(secondMessageMock);

        doNothing().when(receiverMock).complete(firstMessageMock);
        doNothing().when(receiverMock).complete(secondMessageMock);

        sessionWorker.run();

        verify(rankerServiceMock).execute(firstMessageMock);
        verify(rankerServiceMock).execute(secondMessageMock);

        verify(receiverMock).complete(firstMessageMock);
        verify(receiverMock).complete(secondMessageMock);
    }


    @Test
    void run_whenNoMessagesReceived_completesWorker() {
        try (ServiceBusReceiverClient receiverMock = configureSessionReceiver()) {

            when(initiativeCountersServiceMock.hasAvailableBudget(SESSION_ID))
                    .thenReturn(true);

            when(rankerProcessorPropertiesMock.getWaitMessageSeconds())
                    .thenReturn(WAIT_MESSAGE_SECONDS);

            when(
                    receiverMock.receiveMessages(
                            REQUESTED_MESSAGES,
                            Duration.ofSeconds(WAIT_MESSAGE_SECONDS)
                    )
            ).thenReturn(IterableStream.of(List.of()));
        }

        sessionWorker.run();

        verifyNoInteractions(rankerServiceMock);
    }

    private ServiceBusReceiverClient configureSessionReceiver() {
        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder
                sessionReceiverBuilderMock =
                mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);

        ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder
                sessionReceiverBuilderWithQueueMock =
                mock(ServiceBusClientBuilder.ServiceBusSessionReceiverClientBuilder.class);

        ServiceBusSessionReceiverClient sessionReceiverClientMock =
                mock(ServiceBusSessionReceiverClient.class);

        ServiceBusReceiverClient receiverMock =
                mock(ServiceBusReceiverClient.class);

        when(serviceBusClientBuilderMock.sessionReceiver())
                .thenReturn(sessionReceiverBuilderMock);

        when(sessionReceiverBuilderMock.queueName(QUEUE_NAME))
                .thenReturn(sessionReceiverBuilderWithQueueMock);

        when(sessionReceiverBuilderWithQueueMock.disableAutoComplete())
                .thenReturn(sessionReceiverBuilderWithQueueMock);

        when(sessionReceiverBuilderWithQueueMock.buildClient())
                .thenReturn(sessionReceiverClientMock);

        when(sessionReceiverClientMock.acceptSession(SESSION_ID))
                .thenReturn(receiverMock);

        return receiverMock;
    }
}
