package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.common.mongo.MongoTest;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
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

    InitiativeCounters initiativeCounters = InitiativeCounters.builder()
            .id("TEST")
            .initiativeBudgetCents(100000L)
            .onboarded(2L)
            .reservedInitiativeBudgetCents(30000L)
            .residualInitiativeBudgetCents(70000L)
            .build();


    @BeforeEach
    void setUp() {
        initiativeCountersRepository.save(initiativeCounters);
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