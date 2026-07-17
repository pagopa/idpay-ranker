package it.gov.pagopa.ranker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.ranker.processor")
public class RankerProcessorProperties {
    private int maxParallelSessions;
    private int idleTimeoutSeconds;
    private int waitMessageSeconds;
}