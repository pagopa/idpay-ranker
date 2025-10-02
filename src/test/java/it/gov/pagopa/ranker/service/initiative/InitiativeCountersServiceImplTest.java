package it.gov.pagopa.ranker.service.initiative;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.domain.model.Preallocation;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import it.gov.pagopa.ranker.repository.InitiativeCountersAtomicRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InitiativeCountersServiceImplTest {

    private InitiativeCountersAtomicRepository atomicRepository;
    private InitiativeCountersServiceImpl initiativeCountersService;

    @BeforeEach
    void setUp() {
        atomicRepository = mock(InitiativeCountersAtomicRepository.class);
        initiativeCountersService = new InitiativeCountersServiceImpl(atomicRepository);
    }

    @Test
    void testAddedPreallocatedUser_verifyIseeTrue() {
        InitiativeCounters counters = InitiativeCounters.builder()
                .id("initiative1")
                .onboarded(5L)
                .reservedInitiativeBudgetCents(200L)
                .residualInitiativeBudgetCents(800L)
                .preallocationMap(new HashMap<>())
                .build();

        long expectedReservation = initiativeCountersService.calculateReservationCents(true);
        Long sequenceNumber = 123L;
        Long enqueuedTime = 456L;

        when(atomicRepository.incrementOnboardedAndBudget(
                eq("initiative1"),
                eq("user1"),
                eq(expectedReservation),
                eq(sequenceNumber),
                eq(enqueuedTime)))
                .thenAnswer(invocation -> {
                    counters.setOnboarded(counters.getOnboarded() + 1);
                    counters.setReservedInitiativeBudgetCents(counters.getReservedInitiativeBudgetCents() + expectedReservation);
                    counters.setResidualInitiativeBudgetCents(counters.getResidualInitiativeBudgetCents() - expectedReservation);
                    counters.getPreallocationMap().put("user1", Preallocation.builder()
                            .userId("user1")
                            .status(PreallocationStatus.PREALLOCATED)
                            .createdAt(LocalDateTime.now())
                            .sequenceNumber(sequenceNumber)
                            .enqueuedTime(enqueuedTime)
                            .build());
                    return counters;
                });

        long rank = initiativeCountersService.addedPreallocatedUser(
                "initiative1", "user1", true, sequenceNumber, enqueuedTime
        );

        assertEquals(6L, rank);
        assertTrue(counters.getPreallocationMap().containsKey("user1"));
        Preallocation preallocation = counters.getPreallocationMap().get("user1");
        assertEquals("user1", preallocation.getUserId());
        assertEquals(PreallocationStatus.PREALLOCATED, preallocation.getStatus());
        assertEquals(sequenceNumber, preallocation.getSequenceNumber());
        assertEquals(enqueuedTime, preallocation.getEnqueuedTime());
        assertNotNull(preallocation.getCreatedAt());
    }

    @Test
    void testAddedPreallocatedUser_verifyIseeFalse() {
        InitiativeCounters counters = InitiativeCounters.builder()
                .id("initiative2")
                .onboarded(0L)
                .reservedInitiativeBudgetCents(0L)
                .residualInitiativeBudgetCents(1000L)
                .preallocationMap(new HashMap<>())
                .build();

        long expectedReservation = initiativeCountersService.calculateReservationCents(false);
        Long sequenceNumber = 111L;
        Long enqueuedTime = 222L;

        when(atomicRepository.incrementOnboardedAndBudget(
                eq("initiative2"),
                eq("user2"),
                eq(expectedReservation),
                eq(sequenceNumber),
                eq(enqueuedTime)))
                .thenAnswer(invocation -> {
                    counters.setOnboarded(counters.getOnboarded() + 1);
                    counters.setReservedInitiativeBudgetCents(counters.getReservedInitiativeBudgetCents() + expectedReservation);
                    counters.setResidualInitiativeBudgetCents(counters.getResidualInitiativeBudgetCents() - expectedReservation);
                    counters.getPreallocationMap().put("user2", Preallocation.builder()
                            .userId("user2")
                            .status(PreallocationStatus.PREALLOCATED)
                            .createdAt(LocalDateTime.now())
                            .sequenceNumber(sequenceNumber)
                            .enqueuedTime(enqueuedTime)
                            .build());
                    return counters;
                });

        long rank = initiativeCountersService.addedPreallocatedUser(
                "initiative2", "user2", false, sequenceNumber, enqueuedTime
        );

        assertEquals(1L, rank);
        assertTrue(counters.getPreallocationMap().containsKey("user2"));
        Preallocation preallocation = counters.getPreallocationMap().get("user2");
        assertEquals("user2", preallocation.getUserId());
        assertEquals(PreallocationStatus.PREALLOCATED, preallocation.getStatus());
        assertEquals(sequenceNumber, preallocation.getSequenceNumber());
        assertEquals(enqueuedTime, preallocation.getEnqueuedTime());
        assertNotNull(preallocation.getCreatedAt());
    }

    @Test
    void testAddedPreallocatedUser_throwsExceptionWhenAtomicRepositoryReturnsNull() {
        long expectedReservation = initiativeCountersService.calculateReservationCents(true);
        Long sequenceNumber = 1L;
        Long enqueuedTime = 2L;

        when(atomicRepository.incrementOnboardedAndBudget(
                eq("initiativeX"),
                eq("userX"),
                eq(expectedReservation),
                eq(sequenceNumber),
                eq(enqueuedTime)))
                .thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                initiativeCountersService.addedPreallocatedUser("initiativeX", "userX", true, sequenceNumber, enqueuedTime)
        );

        assertEquals("Initiative not found or insufficient budget: initiativeX", exception.getMessage());
    }

    @Test
    void testAddedPreallocatedUserWithNullSequenceAndEnqueuedTime() {
        InitiativeCounters counters = InitiativeCounters.builder()
                .id("initiative3")
                .onboarded(0L)
                .reservedInitiativeBudgetCents(0L)
                .residualInitiativeBudgetCents(1000L)
                .preallocationMap(new HashMap<>())
                .build();

        long expectedReservation = initiativeCountersService.calculateReservationCents(true);

        when(atomicRepository.incrementOnboardedAndBudget(
                eq("initiative3"),
                eq("user3"),
                eq(expectedReservation),
                any(Long.class),
                any(Long.class)
        )).thenAnswer(invocation -> {
            counters.setOnboarded(counters.getOnboarded() + 1);
            counters.setReservedInitiativeBudgetCents(counters.getReservedInitiativeBudgetCents() + expectedReservation);
            counters.setResidualInitiativeBudgetCents(counters.getResidualInitiativeBudgetCents() - expectedReservation);
            counters.getPreallocationMap().put("user3", Preallocation.builder()
                    .userId("user3")
                    .status(PreallocationStatus.PREALLOCATED)
                    .createdAt(LocalDateTime.now())
                    .sequenceNumber(invocation.getArgument(3))
                    .enqueuedTime(invocation.getArgument(4))
                    .build());
            return counters;
        });

        long rank = initiativeCountersService.addedPreallocatedUser("initiative3", "user3", true, null, null);

        assertEquals(1L, rank);
        assertTrue(counters.getPreallocationMap().containsKey("user3"));
    }

    @Test
    void testCalculateReservationCents() {
        assertEquals(200L, initiativeCountersService.calculateReservationCents(true));
        assertEquals(100L, initiativeCountersService.calculateReservationCents(false));
    }
}
