package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCountersPreallocations;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class InitiativeCountersPreallocationsRepositoryExtImplTest  {


    @Mock
    private MongoTemplate mongoTemplate;

    private InitiativeCountersPreallocationsRepositoryExtImpl repository;

    @BeforeEach
    void setUp() {
        repository = new InitiativeCountersPreallocationsRepositoryExtImpl(mongoTemplate);
    }
    @Test
    void findByIdAndStatusThenUpdateStatus() {

        InitiativeCountersPreallocations expected = new InitiativeCountersPreallocations();

        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(InitiativeCountersPreallocations.class)
        )).thenReturn(expected);

        boolean result = repository.findByIdAndStatusThenUpdateStatus(
                "initiative1",
                PreallocationStatus.PREALLOCATED,
                PreallocationStatus.CAPTURED
        );

        assertTrue(result);

        verify(mongoTemplate, times(1)).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(InitiativeCountersPreallocations.class)
        );
    }

    @Test
    void findByIdAndStatusThenUpdateStatusShouldReturnFalseWhenNoDocumentMatches() {
        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(InitiativeCountersPreallocations.class)
        )).thenReturn(null);

        boolean result = repository.findByIdAndStatusThenUpdateStatus(
                "initiative1",
                PreallocationStatus.CAPTURED,
                PreallocationStatus.REFUNDED
        );

        assertFalse(result);
    }
}
