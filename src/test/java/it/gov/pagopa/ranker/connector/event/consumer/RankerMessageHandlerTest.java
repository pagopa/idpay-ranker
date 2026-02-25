package it.gov.pagopa.ranker.connector.event.consumer;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import it.gov.pagopa.ranker.exception.BudgetExhaustedException;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankerMessageHandlerTest {

  @Mock private RankerService rankerService;
  @Mock private ApplicationEventPublisher publisher;
  @Mock private ServiceBusReceivedMessageContext messageContext;
  @Mock private ServiceBusReceivedMessage message;

  @Test
  void handle_success_executesRankerService() {
    RankerMessageHandler handler = new RankerMessageHandler(rankerService, publisher);

    when(messageContext.getMessage()).thenReturn(message);

    handler.handle(messageContext);

    verify(rankerService, times(1)).execute(message);
    verify(publisher, never()).publishEvent(any());
  }

  @Test
  void handle_genericException_doesNotPublishBudgetEvent() {
    RankerMessageHandler handler = new RankerMessageHandler(rankerService, publisher);

    when(messageContext.getMessage()).thenReturn(message);
    doThrow(new RuntimeException("test")).when(rankerService).execute(message);

    handler.handle(messageContext);

    verify(rankerService, times(1)).execute(message);
    verify(publisher, never()).publishEvent(any(BudgetExhaustedEvent.class));
  }

  @Test
  void handle_budgetExhausted_publishesBudgetEvent() {
    RankerMessageHandler handler = new RankerMessageHandler(rankerService, publisher);

    when(messageContext.getMessage()).thenReturn(message);
    doThrow(new BudgetExhaustedException("test")).when(rankerService).execute(message);

    handler.handle(messageContext);

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(publisher, times(1)).publishEvent(captor.capture());

    BudgetExhaustedEvent ev = (BudgetExhaustedEvent) captor.getValue();
    assertEquals("initiative budget exhausted", ev.reason());
    assertNotNull(ev.occurredAt());
  }
}
