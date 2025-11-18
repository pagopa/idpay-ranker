package it.gov.pagopa.ranker.connector.event.consumer;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import it.gov.pagopa.ranker.exception.BudgetExhaustedException;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankerConsumerClientTest {

    @Mock
    private RankerService rankerService;
    @Mock
    private InitiativeCountersService initiativeCountersService;
    @Mock
    private ServiceBusProcessorClient processorClient;
    @Mock
    private ServiceBusReceivedMessageContext messageContext;
    @Mock
    private ServiceBusReceivedMessage message;

    private RankerConsumerClient rankerConsumerClient;

    @BeforeEach
    void setup() throws Exception {
        rankerConsumerClient = new RankerConsumerClient(
                rankerService,
                initiativeCountersService,
                "Endpoint=sb://fake-servicebus.servicebus.windows.net/;SharedAccessKeyName=FakeKeyName;SharedAccessKey=FakeKey123FakeKey123FakeKey123FakeKey123=;EntityPath=fake-queue",
                "fake-queue",
                false
        );

        setPrivateField(rankerConsumerClient, "connectionString", "Endpoint=sb://fake-servicebus.servicebus.windows.net/;SharedAccessKeyName=FakeKeyName;SharedAccessKey=FakeKey123FakeKey123FakeKey123FakeKey123=;EntityPath=fake-queue");
        setPrivateField(rankerConsumerClient, "queueName", "fake-queue");
        setPrivateField(rankerConsumerClient, "processorClient", processorClient);
        setPrivateField(rankerConsumerClient, "forceStopped", false);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = RankerConsumerClient.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testInit() {
        when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);

        rankerConsumerClient.init();

        verify(processorClient, atMost(1)).start();
    }

    @Test
    void testHandleMessage_success() throws Exception {
        when(messageContext.getMessage()).thenReturn(message);

        Method handleMessage = RankerConsumerClient.class
                .getDeclaredMethod("handleMessage", ServiceBusReceivedMessageContext.class);
        handleMessage.setAccessible(true);
        handleMessage.invoke(rankerConsumerClient, messageContext);

        verify(rankerService, times(1)).execute(message);
        verify(processorClient, never()).close();
    }

    @Test
    void testHandleMessage_genericException() throws Exception {
        when(messageContext.getMessage()).thenReturn(message);
        doThrow(new RuntimeException("test error")).when(rankerService).execute(message);

        Method handleMessage = RankerConsumerClient.class
                .getDeclaredMethod("handleMessage", ServiceBusReceivedMessageContext.class);
        handleMessage.setAccessible(true);
        handleMessage.invoke(rankerConsumerClient, messageContext);

        verify(rankerService).execute(message);
        verify(processorClient, never()).close();
    }

    @Test
    void testHandleMessage_BudgetExhausted() throws Exception {
        when(messageContext.getMessage()).thenReturn(message);
        doThrow(new BudgetExhaustedException("test error")).when(rankerService).execute(message);

        Method handleMessage = RankerConsumerClient.class
                .getDeclaredMethod("handleMessage", ServiceBusReceivedMessageContext.class);
        handleMessage.setAccessible(true);

        when(processorClient.isRunning()).thenReturn(true);
        handleMessage.invoke(rankerConsumerClient, messageContext);

        verify(rankerService, times(1)).execute(message);
        verify(processorClient, times(1)).close();
    }

    @Test
    void testStopConsumer_whenRunning() {
        when(processorClient.isRunning()).thenReturn(true);

        rankerConsumerClient.stopConsumer();

        verify(processorClient).close();
    }

    @Test
    void testStopConsumer_whenNotRunning() {
        when(processorClient.isRunning()).thenReturn(false);

        rankerConsumerClient.stopConsumer();

        verify(processorClient, never()).close();
    }

    @Test
    void testStartConsumer_whenHasBudgetAndNotRunning() {
        when(processorClient.isRunning()).thenReturn(false);
        when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);

        rankerConsumerClient.startConsumer();

        verify(processorClient).start();
    }

    @Test
    void testStartConsumer_whenAlreadyRunning() {
        when(processorClient.isRunning()).thenReturn(true);

        rankerConsumerClient.startConsumer();

        verify(processorClient, never()).start();
    }

    @Test
    void testCheckResidualBudgetAndStartConsumer_whenHasBudgetAndNotRunning() {
        when(processorClient.isRunning()).thenReturn(false);
        when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);

        rankerConsumerClient.checkResidualBudgetAndStartConsumer();

        verify(processorClient).start();
    }

    @Test
    void testCheckResidualBudgetAndStartConsumer_whenNoBudget() {
        when(processorClient.isRunning()).thenReturn(false);
        when(initiativeCountersService.hasAvailableBudget()).thenReturn(false);

        rankerConsumerClient.checkResidualBudgetAndStartConsumer();

        verify(processorClient, never()).start();
    }

    @Test
    void testStartConsumer_whenProcessorClientIsNull() throws Exception {
        RankerConsumerClient client = new RankerConsumerClient(
                rankerService,
                initiativeCountersService,
                "fake-connection",
                "fake-queue",
                false
        );

        java.lang.reflect.Field processorClientField = RankerConsumerClient.class
                .getDeclaredField("processorClient");
        processorClientField.setAccessible(true);
        processorClientField.set(client, null);

        client.startConsumer();

        verify(processorClient, never()).start();
    }


    @Test
    void testCheckResidualBudgetAndStartConsumer_whenForceStopped() throws Exception {
        RankerConsumerClient clientWithForceStopped = new RankerConsumerClient(
                rankerService,
                initiativeCountersService,
                "fake-connection",
                "fake-queue",
                true
        );

        java.lang.reflect.Field processorClientField = RankerConsumerClient.class
                .getDeclaredField("processorClient");
        processorClientField.setAccessible(true);
        processorClientField.set(clientWithForceStopped, processorClient);

        when(processorClient.isRunning()).thenReturn(false);
        when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);

        clientWithForceStopped.checkResidualBudgetAndStartConsumer();

        verify(processorClient, never()).start();
    }

    @Test
    void testCheckResidualBudgetAndStartConsumer_whenProcessorRunning() {
        when(processorClient.isRunning()).thenReturn(true);
        when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);

        rankerConsumerClient.checkResidualBudgetAndStartConsumer();

        verify(processorClient, never()).start();
    }

    @Test
    void testStopConsumer_processorClientExistsButNotRunning() throws Exception {
        RankerConsumerClient client = new RankerConsumerClient(
                rankerService,
                initiativeCountersService,
                "fake-connection",
                "fake-queue",
                false
        );

        Field processorField = RankerConsumerClient.class.getDeclaredField("processorClient");
        processorField.setAccessible(true);
        processorField.set(client, processorClient);

        when(processorClient.isRunning()).thenReturn(false);

        client.stopConsumer();

        verify(processorClient, never()).close();
    }

    @Test
    void testStartConsumer_processorClientNull_noStart() {
        RankerConsumerClient client = new RankerConsumerClient(
                rankerService,
                initiativeCountersService,
                "fake-connection",
                "fake-queue",
                false
        );

        client.startConsumer();

        verify(processorClient, never()).start();
    }
}
