package it.gov.pagopa.common.config;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
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

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertEquals("UP", health.getStatus().getCode());
        assertEquals(10, health.getDetails().get("maxWireVersion"));
    }

    @Test
    void testDoHealthCheck_commandThrowsException_shouldPropagate() {
        // Arrange
        RuntimeException exception = new RuntimeException("Mongo error");
        when(mongoTemplate.executeCommand("{ isMaster: 1 }")).thenThrow(exception);

        // Act
        Health health = healthIndicator.health();

        // Assert: indicator reports DOWN and includes error message
        assertEquals("DOWN", health.getStatus().getCode());
        Object errorDetail = health.getDetails().get("error");
        String errorString = java.util.Objects.toString(errorDetail, "");
        org.junit.jupiter.api.Assertions.assertTrue(errorString.contains("Mongo error"));
    }

}