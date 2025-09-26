package it.gov.pagopa.common.mongo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.boot.actuate.data.mongo.MongoHealthIndicator;

@Configuration
public class MongoHealthConfig {

    @Bean
    public MongoHealthIndicator customMongoHealthIndicator(MongoTemplate mongoTemplate) {
        return new MongoHealthIndicator(mongoTemplate);
    }
}
