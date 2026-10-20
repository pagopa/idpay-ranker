package it.gov.pagopa.ranker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.ranker.manual-dequeue")
public class RankerManualDequeueProperties {
    private int corePoolSize;
    private int maxPoolSize;
    private int queueCapacity;
}