//package it.gov.pagopa.ranker.repository;
//
//import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
//import it.gov.pagopa.ranker.domain.model.Preallocation;
//import it.gov.pagopa.ranker.enums.PreallocationStatus;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.mongodb.core.FindAndModifyOptions;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.data.mongodb.core.query.Update;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class InitiativeCountersAtomicRepositoryImplTest {
//
//    @Mock
//    private MongoTemplate mongoTemplate;
//
//    private InitiativeCountersAtomicRepositoryImpl repository;
//
//    @BeforeEach
//    void setUp() {
//        repository = new InitiativeCountersAtomicRepositoryImpl(mongoTemplate);
//    }
//
//    @Test
//    void testIncrementOnboardedAndBudget() {
//        Long sequenceNumber = 123L;
//        Long enqueuedTime = 456L;
//
//        InitiativeCounters expected = InitiativeCounters.builder()
//                .id("initiative1")
//                .onboarded(1L)
//                .reservedInitiativeBudgetCents(100L)
//                .residualInitiativeBudgetCents(900L)
//                .preallocationMap(new HashMap<>())
//                .build();
//
//        expected.getPreallocationMap().put("user1",
//                Preallocation.builder()
//                        .userId("user1")
//                        .status(PreallocationStatus.PREALLOCATED)
//                        .createdAt(LocalDateTime.now())
//                        .sequenceNumber(sequenceNumber)
//                        .enqueuedTime(enqueuedTime)
//                        .build()
//        );
//
//        when(mongoTemplate.findAndModify(
//                any(Query.class),
//                any(Update.class),
//                any(FindAndModifyOptions.class),
//                eq(InitiativeCounters.class)
//        )).thenReturn(expected);
//
//        InitiativeCounters result = repository.incrementOnboardedAndBudget(
//                "initiative1",
//                "user1",
//                100L,
//                sequenceNumber,
//                enqueuedTime
//        );
//
//        assertNotNull(result);
//        assertEquals(expected, result);
//        assertTrue(result.getPreallocationMap().containsKey("user1"));
//        Preallocation preallocation = result.getPreallocationMap().get("user1");
//        assertEquals("user1", preallocation.getUserId());
//        assertEquals(PreallocationStatus.PREALLOCATED, preallocation.getStatus());
//        assertEquals(sequenceNumber, preallocation.getSequenceNumber());
//        assertEquals(enqueuedTime, preallocation.getEnqueuedTime());
//        assertNotNull(preallocation.getCreatedAt());
//
//        verify(mongoTemplate, times(1)).findAndModify(
//                any(Query.class),
//                any(Update.class),
//                any(FindAndModifyOptions.class),
//                eq(InitiativeCounters.class)
//        );
//    }
//}
