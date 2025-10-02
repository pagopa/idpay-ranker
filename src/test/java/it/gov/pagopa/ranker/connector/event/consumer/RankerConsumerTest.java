package it.gov.pagopa.ranker.connector.event.consumer;

import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.exception.MessageProcessingException;
import it.gov.pagopa.ranker.service.ranker.RankerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RankerConsumerTest {

    private RankerServiceImpl rankerServiceImpl;
    private RankerConsumer rankerConsumer;

    @BeforeEach
    void setUp() {
        rankerServiceImpl = mock(RankerServiceImpl.class);
        rankerConsumer = new RankerConsumer(rankerServiceImpl);
    }

    @Test
    void testRankerProcessorCallsService() {
        OnboardingDTO dto = OnboardingDTO.builder()
                .userId("u1")
                .initiativeId("i1")
                .build();

        Message<OnboardingDTO> message = MessageBuilder.withPayload(dto)
                .setHeader("sequenceNumber", 123L)
                .setHeader("enqueuedTime", 456L)
                .build();

        var consumer = rankerConsumer.rankerProcessor();
        consumer.accept(message);

        verify(rankerServiceImpl, times(1)).execute(eq(dto), eq(123L), eq(456L));
    }

    @Test
    void testRankerProcessorHandlesMissingHeaders() {
        OnboardingDTO dto = OnboardingDTO.builder()
                .userId("u1")
                .initiativeId("i1")
                .build();

        Message<OnboardingDTO> message = MessageBuilder.withPayload(dto)
                .setHeader("sequenceNumber", "notANumber")
                .setHeader("enqueuedTime", null)
                .build();

        var consumer = rankerConsumer.rankerProcessor();
        consumer.accept(message);

        verify(rankerServiceImpl, times(1)).execute(eq(dto), eq(0L), eq(0L));
    }

    @Test
    void testRankerProcessorWrapsUnexpectedException() {
        OnboardingDTO dto = OnboardingDTO.builder()
                .userId("u1")
                .initiativeId("i1")
                .build();

        Message<OnboardingDTO> message = MessageBuilder.withPayload(dto).build();

        doThrow(new RuntimeException("something went wrong"))
                .when(rankerServiceImpl).execute(any(), anyLong(), anyLong());

        var consumer = rankerConsumer.rankerProcessor();

        MessageProcessingException ex = assertThrows(MessageProcessingException.class,
                () -> consumer.accept(message));

        assertEquals("Error processing message", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex.getCause());
        assertEquals("something went wrong", ex.getCause().getMessage());
    }

}
