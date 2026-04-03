package it.gov.pagopa.ranker.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@TestPropertySource( properties = {
        "app.ranker.processor.max-parallel-sessions=2",
        "app.ranker.processor.idle-timeout-seconds=120",
        "app.ranker.processor.reconcile-delay-ms = 300"

})
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = RankerProcessorProperties.class)
class RankerProcessorPropertiesTest {
    @Value("${app.ranker.processor.max-parallel-sessions}")
    private int maxParallelSessions;
    @Value("${app.ranker.processor.idle-timeout-seconds}")
    private int idleTimeoutSeconds;
    @Value("${app.ranker.processor.reconcile-delay-ms}")
    private long reconcileDelayMs;

    @Autowired
    private RankerProcessorProperties properties;

    @Test
    void properties(){
        Assertions.assertEquals(maxParallelSessions, properties.getMaxParallelSessions());
        Assertions.assertEquals(idleTimeoutSeconds, properties.getIdleTimeoutSeconds());
        Assertions.assertEquals(reconcileDelayMs, properties.getReconcileDelayMs());
    }

}