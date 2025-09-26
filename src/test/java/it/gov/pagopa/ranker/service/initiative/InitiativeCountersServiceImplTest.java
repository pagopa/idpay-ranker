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
    void testAddedPreallocatedUserIncrementsOnboardedAndBudget_verifyIseeTrue() {
        InitiativeCounters counters = InitiativeCounters.builder()
                .id("initiative1")
                .onboarded(5L)
                .reservedInitiativeBudgetCents(200L)
                .residualInitiativeBudgetCents(800L)
                .preallocationMap(new HashMap<>())
                .build();

        long expectedReservation = initiativeCountersService.calculateReservationCents(true);

        when(atomicRepository.incrementOnboardedAndBudget("initiative1", "user1", expectedReservation)).thenAnswer(invocation -> {
            counters.setOnboarded(counters.getOnboarded() + 1);
            counters.setReservedInitiativeBudgetCents(counters.getReservedInitiativeBudgetCents() + expectedReservation);
            counters.setResidualInitiativeBudgetCents(counters.getResidualInitiativeBudgetCents() - expectedReservation);
            counters.getPreallocationMap().put("user1", Preallocation.builder()
                    .userId("user1")
                    .status(PreallocationStatus.PREALLOCATED)
                    .createdAt(LocalDateTime.now())
                    .build());
            return counters;
        });

        long rank = initiativeCountersService.addedPreallocatedUser("initiative1", "user1", true);

        assertEquals(6L, rank);
        assertEquals(1, counters.getPreallocationMap().size());
        Preallocation preallocation = counters.getPreallocationMap().get("user1");
        assertEquals("user1", preallocation.getUserId());
        assertEquals(PreallocationStatus.PREALLOCATED, preallocation.getStatus());
        assertNotNull(preallocation.getCreatedAt());
    }

    @Test
    void testAddedPreallocatedUserIncrementsOnboardedAndBudget_verifyIseeFalse() {
        InitiativeCounters counters = InitiativeCounters.builder()
                .id("initiative2")
                .onboarded(0L)
                .reservedInitiativeBudgetCents(0L)
                .residualInitiativeBudgetCents(1000L)
                .preallocationMap(new HashMap<>())
                .build();

        long expectedReservation = initiativeCountersService.calculateReservationCents(false);

        when(atomicRepository.incrementOnboardedAndBudget("initiative2", "user2", expectedReservation)).thenAnswer(invocation -> {
            counters.setOnboarded(counters.getOnboarded() + 1);
            counters.setReservedInitiativeBudgetCents(counters.getReservedInitiativeBudgetCents() + expectedReservation);
            counters.setResidualInitiativeBudgetCents(counters.getResidualInitiativeBudgetCents() - expectedReservation);
            counters.getPreallocationMap().put("user2", Preallocation.builder()
                    .userId("user2")
                    .status(PreallocationStatus.PREALLOCATED)
                    .createdAt(LocalDateTime.now())
                    .build());
            return counters;
        });

        long rank = initiativeCountersService.addedPreallocatedUser("initiative2", "user2", false);

        assertEquals(1L, rank);
        assertEquals(1, counters.getPreallocationMap().size());
        Preallocation preallocation = counters.getPreallocationMap().get("user2");
        assertEquals("user2", preallocation.getUserId());
        assertEquals(PreallocationStatus.PREALLOCATED, preallocation.getStatus());
        assertNotNull(preallocation.getCreatedAt());
    }

    @Test
    void testAddedPreallocatedUser_throwsExceptionWhenAtomicRepositoryReturnsNull() {
        long expectedReservation = initiativeCountersService.calculateReservationCents(true);

        when(atomicRepository.incrementOnboardedAndBudget("initiativeX", "userX", expectedReservation)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                initiativeCountersService.addedPreallocatedUser("initiativeX", "userX", true)
        );

        assertEquals("Initiative not found or insufficient budget: initiativeX", exception.getMessage());
    }

    @Test
    void testCalculateReservationCents() {
        assertEquals(200L, initiativeCountersService.calculateReservationCents(true));
        assertEquals(100L, initiativeCountersService.calculateReservationCents(false));
    }
}
