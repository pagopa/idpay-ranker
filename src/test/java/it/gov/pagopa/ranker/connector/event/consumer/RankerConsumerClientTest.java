package it.gov.pagopa.ranker.connector.event.consumer;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import it.gov.pagopa.ranker.exception.BudgetExhaustedException;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private RankerConsumerClient rankerConsumerClient;

    @Test
    void testInit() throws Exception {
        java.lang.reflect.Field connectionStringField = RankerConsumerClient.class
                .getDeclaredField("connectionString");
        connectionStringField.setAccessible(true);
        connectionStringField.set(rankerConsumerClient, "Endpoint=sb://fake-servicebus.servicebus.windows.net/;SharedAccessKeyName=FakeKeyName;SharedAccessKey=FakeKey123FakeKey123FakeKey123FakeKey123=;EntityPath=fake-queue");

        java.lang.reflect.Field queueNameField = RankerConsumerClient.class
                .getDeclaredField("queueName");
        queueNameField.setAccessible(true);
        queueNameField.set(rankerConsumerClient, "fake-queue");

        java.lang.reflect.Field processorClientField = RankerConsumerClient.class
                .getDeclaredField("processorClient");
        processorClientField.setAccessible(true);
        processorClientField.set(rankerConsumerClient, processorClient);

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

        setClientStateModificable();
        when(processorClient.isRunning()).thenReturn(true);
        handleMessage.invoke(rankerConsumerClient, messageContext);

        verify(rankerService, times(1)).execute(message);
        verify(processorClient, times(1)).close();
    }

    @Test
    void testStopConsumer_whenRunning() throws NoSuchFieldException, IllegalAccessException {
        setClientStateModificable();

        when(processorClient.isRunning()).thenReturn(true);

        rankerConsumerClient.stopConsumer();

        verify(processorClient).close();
    }

    private void setClientStateModificable() throws NoSuchFieldException, IllegalAccessException {
        java.lang.reflect.Field processorClientField = RankerConsumerClient.class
                .getDeclaredField("processorClient");
        processorClientField.setAccessible(true);
        processorClientField.set(rankerConsumerClient, processorClient);
    }

    @Test
    void testStopConsumer_whenNotRunning() throws NoSuchFieldException, IllegalAccessException {
        setClientStateModificable();

        when(processorClient.isRunning()).thenReturn(false);

        rankerConsumerClient.startConsumer();

        verify(processorClient, never()).start();
    }

    @Test
    void testStartConsumer_whenHasBudgetAndNotRunning() throws NoSuchFieldException, IllegalAccessException {
        setClientStateModificable();

        when(processorClient.isRunning()).thenReturn(false);
        when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);

        rankerConsumerClient.startConsumer();

        verify(processorClient).start();
    }

    @Test
    void testStartConsumer_whenAlreadyRunning() throws NoSuchFieldException, IllegalAccessException {
        setClientStateModificable();

        when(processorClient.isRunning()).thenReturn(true);

        rankerConsumerClient.startConsumer();

        verify(processorClient, never()).start();
    }

    @Test
    void testCheckResidualBudgetAndStartConsumer_whenHasBudgetAndNotRunning() throws NoSuchFieldException, IllegalAccessException {
        setClientStateModificable();

        when(processorClient.isRunning()).thenReturn(false);
        when(initiativeCountersService.hasAvailableBudget()).thenReturn(true);

        rankerConsumerClient.checkResidualBudgetAndStartConsumer();

        verify(processorClient).start();
    }

    @Test
    void testCheckResidualBudgetAndStartConsumer_whenNoBudget() throws IllegalAccessException, NoSuchFieldException {
        setClientStateModificable();

        when(processorClient.isRunning()).thenReturn(false);
        when(initiativeCountersService.hasAvailableBudget()).thenReturn(false);

        rankerConsumerClient.checkResidualBudgetAndStartConsumer();

        verify(processorClient, never()).start();
    }
}
