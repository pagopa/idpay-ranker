package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.common.mongo.MongoTest;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.domain.model.Preallocation;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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
        Optional<InitiativeCounters> resultOpt = initiativeCountersRepository.findById("TEST");

        assertFalse(resultOpt.isEmpty());
        InitiativeCounters result = resultOpt.get();
        assertEquals(initiativeCounters.getInitiativeBudgetCents(),result.getInitiativeBudgetCents());
        assertEquals(initiativeCounters.getResidualInitiativeBudgetCents(),result.getResidualInitiativeBudgetCents());
        assertEquals(initiativeCounters.getReservedInitiativeBudgetCents(),result.getReservedInitiativeBudgetCents());
        assertTrue(result.getPreallocationMap().isEmpty());
    }

    @Test
    void findById_noRetrieveInitiative() {
        Optional<InitiativeCounters> resultOpt = initiativeCountersRepository.findById("ANOTHER_INITIATIVE");

        assertTrue(resultOpt.isEmpty());
    }
}