package it.gov.pagopa.common.config;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomMongoHealthIndicatorTest {
    @Mock
    private MongoTemplate mongoTemplate;
    private CustomMongoHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new CustomMongoHealthIndicator(mongoTemplate);
    }

    @Test
    void testConstructor_nullMongoTemplate_shouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CustomMongoHealthIndicator(null)
        );
        assertEquals("MongoTemplate must not be null", exception.getMessage());
    }

    @Test
    void testDoHealthCheck_shouldReturnUpStatusWithMaxWireVersion() throws Exception {
        // Arrange
        Document mockResult = new Document();
        mockResult.put("maxWireVersion", 10);
        when(mongoTemplate.executeCommand("{ isMaster: 1 }")).thenReturn(mockResult);

        Health.Builder builder = new Health.Builder();

        // Act
        healthIndicator.doHealthCheck(builder);
        Health health = builder.build();

        // Assert
        assertEquals("UP", health.getStatus().getCode());
        assertEquals(10, health.getDetails().get("maxWireVersion"));
    }

    @Test
    void testDoHealthCheck_commandThrowsException_shouldPropagate() {
        // Arrange
        RuntimeException exception = new RuntimeException("Mongo error");
        when(mongoTemplate.executeCommand("{ isMaster: 1 }")).thenThrow(exception);

        Health.Builder builder = new Health.Builder();

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> healthIndicator.doHealthCheck(builder));
        assertEquals("Mongo error", thrown.getMessage());
    }

}