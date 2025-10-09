package it.gov.pagopa.common.mongo.config;

import it.gov.pagopa.common.config.CustomMongoHealthIndicator;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = MongoHealthConfig.class)
class MongoHealthConfigTest {

    @MockitoBean
    private MongoTemplate mongoTemplate;

    @Autowired
    private CustomMongoHealthIndicator customMongoHealthIndicator;

    @Test
    void testCustomMongoHealthIndicatorBeanCreation() {
        assertNotNull(customMongoHealthIndicator, "CustomMongoHealthIndicator bean should be created");
    }

    @Test
    void testCustomMongoHealthIndicatorUsesMongoTemplate() throws Exception {
        //Given
        Document doc = new Document("maxWireVersion", 10);
        when(mongoTemplate.executeCommand("{ isMaster: 1 }")).thenReturn(doc);

        Health.Builder builder = new Health.Builder();

        Method method = CustomMongoHealthIndicator.class.getDeclaredMethod("doHealthCheck", Health.Builder.class);
        method.setAccessible(true); // Rende il metodo invocabile

        //When
        method.invoke(customMongoHealthIndicator, builder);

        //Then
        Health health = builder.build();
        assertEquals("UP", health.getStatus().getCode());
        assertEquals(10, health.getDetails().get("maxWireVersion"));

    }

}