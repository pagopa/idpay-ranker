package it.gov.pagopa.ranker.connector.event.consumer;

import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaseKafkaConsumerTest {

    private TestKafkaConsumer consumer;

    private static class TestKafkaConsumer extends BaseKafkaConsumer {

        private final AtomicBoolean processCalled;
        private final String flowName;

        TestKafkaConsumer(String applicationName, AtomicBoolean processCalled, String flowName) {
            super(applicationName);
            this.processCalled = processCalled;
            this.flowName = flowName;
        }

        @Override
        protected void process(Message<String> message) {
            processCalled.set(true);
        }

        @Override
        protected String getFlowName() {
            return flowName;
        }
    }

    private AtomicBoolean processCalled;

    @BeforeEach
    void setup() {
        processCalled = new AtomicBoolean(false);
        String flowName = "TEST_FLOW";
        consumer = new TestKafkaConsumer("TEST_APP", processCalled, flowName);
    }

    @Test
    void testExecute_CallsProcess_WhenNotRetry() {
        Message<String> message = MessageBuilder.withPayload("payload").build();

        consumer.execute(message);

        assertTrue(processCalled.get(), "Process should be called when not retry");
    }

    @Test
    void testExecute_DoesNotCallProcess_WhenRetryFromOtherApp() {
        byte[] otherApp = "OTHER_APP".getBytes(StandardCharsets.UTF_8);
        Message<String> message = MessageBuilder.withPayload("payload")
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, otherApp)
                .build();

        consumer.execute(message);

        assertFalse(processCalled.get(), "Process should NOT be called for retry from other app");
    }


    @Test
    void testDoFinally_ExecutesWithoutException() {
        Message<String> message = MessageBuilder.withPayload("payload").build();

        assertDoesNotThrow(() -> consumer.execute(message));
        assertTrue(processCalled.get(), "Process should be called");
    }

    @Test
    void testExecute_AcknowledgeCalled() {
        AtomicBoolean acknowledged = new AtomicBoolean(false);
        Acknowledgment ack = mock(Acknowledgment.class);
        doAnswer(invocation -> {
            acknowledged.set(true);
            return null;
        }).when(ack).acknowledge();

        Message<String> message = MessageBuilder.withPayload("payload")
                .setHeader(KafkaHeaders.ACKNOWLEDGMENT, ack)
                .build();

        consumer.execute(message);

        assertTrue(acknowledged.get(), "Acknowledgment should be called");
        verify(ack).acknowledge();
    }
}
