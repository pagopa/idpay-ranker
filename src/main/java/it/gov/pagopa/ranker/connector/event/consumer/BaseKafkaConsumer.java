package it.gov.pagopa.ranker.connector.event.consumer;

import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import reactor.util.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class BaseKafkaConsumer {
    protected static final String CONTEXT_KEY_START_TIME = "START_TIME";
    /** Key used inside the {@link Context} to store a msg identifier used for logging purpose */
    protected static final String CONTEXT_KEY_MSG_ID = "MSG_ID";
    protected final String applicationName;

    protected BaseKafkaConsumer(String applicationName) {
        this.applicationName = applicationName;
    }

    private boolean isRetryFromOtherApps(Message<String> message, String flowName) {
        byte[] retryingApplicationName = message.getHeaders().get(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, byte[].class);
        if(retryingApplicationName != null && !new String(retryingApplicationName, StandardCharsets.UTF_8).equals(this.applicationName)){
            log.info("[{}] Discarding message due to other application retry ({}): {}", flowName, new String(retryingApplicationName, StandardCharsets.UTF_8), message.getPayload());
            return true;
        }
        return false;
    }

    private void acknowledgeMessage(Message<String> message) {
        Acknowledgment ack = message.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
        if (ack != null) {
            ack.acknowledge();
        }
    }

    public void execute(Message<String> message) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put(CONTEXT_KEY_START_TIME, System.currentTimeMillis());
        ctx.put(CONTEXT_KEY_MSG_ID, message.getPayload());

        if (!isRetryFromOtherApps(message, getFlowName())) {
            this.process(message);
        }

        acknowledgeMessage(message);
        doFinally(ctx);

    }

    protected void doFinally(Map<String, Object> ctx) {
        Long startTime = (Long)ctx.get(CONTEXT_KEY_START_TIME);
        String msgId = (String)ctx.get(CONTEXT_KEY_MSG_ID);
        if(startTime != null){
            log.info("[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms {}", getFlowName(), System.currentTimeMillis() - startTime, msgId);
        }
    }

    protected abstract void process(Message<String> message);
    protected abstract String getFlowName();
}
