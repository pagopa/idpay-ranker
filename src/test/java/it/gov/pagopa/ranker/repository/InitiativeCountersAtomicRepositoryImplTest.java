package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitiativeCountersAtomicRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private InitiativeCountersAtomicRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new InitiativeCountersAtomicRepositoryImpl(mongoTemplate);
    }

    @Test
    void testIncrementOnboardedAndBudget() {
        LocalDateTime now = LocalDateTime.now();
        Long sequenceNumber = 123L;

        InitiativeCounters expected = InitiativeCounters.builder()
                .id("initiative1")
                .onboarded(1L)
                .reservedInitiativeBudgetCents(100L)
                .residualInitiativeBudgetCents(900L)
                .build();

        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(InitiativeCounters.class)
        )).thenReturn(expected);

        InitiativeCounters result = repository.incrementOnboardedAndBudget(
                "initiative1",
                100L
        );

        assertNotNull(result);
        assertEquals(expected, result);

        verify(mongoTemplate, times(1)).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(InitiativeCounters.class)
        );
    }

    @Test
    void testDecrementOnboardedAndBudget() {
        InitiativeCounters expected = InitiativeCounters.builder()
                .id("initiative1")
                .onboarded(1L)
                .reservedInitiativeBudgetCents(0L)
                .residualInitiativeBudgetCents(1000L)
                .build();

        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(InitiativeCounters.class)
        )).thenReturn(expected);

        InitiativeCounters result = repository.decrementOnboardedAndBudget("initiative1", 100L);

        assertNotNull(result);
        assertEquals(expected, result);

        verify(mongoTemplate, times(1)).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(InitiativeCounters.class)
        );
    }

}
