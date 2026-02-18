package it.gov.pagopa.common.config;

import org.bson.Document;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.Assert;

public class CustomMongoHealthIndicator implements HealthIndicator {

    private final MongoTemplate mongoTemplate;

    public CustomMongoHealthIndicator(MongoTemplate mongoTemplate) {
        Assert.notNull(mongoTemplate, "MongoTemplate must not be null");
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Health health() {
        try {
            Document result = this.mongoTemplate.executeCommand("{ isMaster: 1 }");
            return Health.up().withDetail("maxWireVersion", result.getInteger("maxWireVersion")).build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }

}
