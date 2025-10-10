package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.common.mongo.MongoTest;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.domain.model.Preallocation;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
@MongoTest
class InitiativeCountersRepositoryTest {

    @Autowired
    protected InitiativeCountersRepository initiativeCountersRepository;

    Map<String, Preallocation> userPreallocated = Map.of(
            "USER1", Preallocation.builder().userId("USER1").status(PreallocationStatus.PREALLOCATED).build(),
            "USER2", Preallocation.builder().userId("USER2").status(PreallocationStatus.PREALLOCATED).build());

    InitiativeCounters initiativeCounters = InitiativeCounters.builder()
            .id("TEST")
            .initiativeBudgetCents(100000L)
            .onboarded(2L)
            .reservedInitiativeBudgetCents(30000L)
            .residualInitiativeBudgetCents(70000L)
            .preallocationMap(userPreallocated)
            .build();


    @BeforeEach
    void setUp() {
        initiativeCountersRepository.save(initiativeCounters);
    }

    @Test
    void existsByInitiativeIdAndUserId_UserAlreadyPreallocated() {
        boolean result = initiativeCountersRepository.existsByInitiativeIdAndUserId("TEST", "USER1");
        assertTrue(result);
    }

    @Test
    void existsByInitiativeIdAndUserId_UserNotPreallocated() {
        boolean result = initiativeCountersRepository.existsByInitiativeIdAndUserId("TEST", "USER3");
        assertFalse(result);
    }


    @Test
    void findById_retrieveInitiativeOk() {
        boolean result = initiativeCountersRepository.existsByIdInAndResidualInitiativeBudgetCentsGreaterThanEqual(List.of("TEST"), 10000);

        assertTrue(result);
    }

    @Test
    void findById_noRetrieveInitiative() {
        boolean result = initiativeCountersRepository.existsByIdInAndResidualInitiativeBudgetCentsGreaterThanEqual(List.of("ANOTHER_INITIATIVE"),10000);

        assertFalse(result);
    }
}