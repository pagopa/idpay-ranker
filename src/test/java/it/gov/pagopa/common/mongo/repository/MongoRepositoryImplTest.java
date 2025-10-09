package it.gov.pagopa.common.mongo.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MongoRepositoryImplTest {
    private MongoOperations mongoOperations;
    private MongoEntityInformation<TestEntity, String> entityInformation;
    private MongoRepositoryImpl<TestEntity, String> repository;

    static class TestEntity {
        private String id;
        private String name;

        public TestEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public String getName() { return name; }
    }

    @BeforeEach
    void setUp() {
        mongoOperations = mock(MongoOperations.class);
        entityInformation = mock(MongoEntityInformation.class);

        when(entityInformation.getJavaType()).thenReturn(TestEntity.class);
        when(entityInformation.getCollectionName()).thenReturn("testCollection");

        repository = new MongoRepositoryImpl<>(entityInformation, mongoOperations);
    }

    @Test
    void testFindById_entityFound_shouldReturnOptionalWithEntity() {
        TestEntity entity = new TestEntity("1", "Test Name");
        List<TestEntity> mockResult = List.of(entity);

        when(mongoOperations.find(any(Query.class), eq(TestEntity.class), eq("testCollection")))
                .thenReturn(mockResult);

        Optional<TestEntity> result = repository.findById("1");

        assertTrue(result.isPresent());
        assertEquals("1", result.get().getId());
        assertEquals("Test Name", result.get().getName());
    }

    @Test
    void testFindById_entityNotFound_shouldReturnEmptyOptional() {
        when(mongoOperations.find(any(Query.class), eq(TestEntity.class), eq("testCollection")))
                .thenReturn(Collections.emptyList());

        Optional<TestEntity> result = repository.findById("2");

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindById_verifyQueryExecution() {
        TestEntity entity = new TestEntity("3", "Another Test");
        when(mongoOperations.find(any(Query.class), eq(TestEntity.class), eq("testCollection")))
                .thenReturn(List.of(entity));

        repository.findById("3");

        verify(mongoOperations, times(1))
                .find(any(Query.class), eq(TestEntity.class), eq("testCollection"));
    }
}