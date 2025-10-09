package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.common.mongo.MongoTest;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

@MongoTest
class InitiativeCountersReservationOpsRepositoryImplTest {

    @Autowired
    protected InitiativeCountersRepository initiativeCountersRepository;
    @Autowired
    private InitiativeCountersAtomicRepositoryImpl initiativeCountersReservationOpsRepositoryImpl;

    @Test
    void testReservation() {
        int N = 1000;

        final BigDecimal budget = BigDecimal.valueOf(100099);
        final Long budgetReservedPerRequestCents = euro2cents( BigDecimal.valueOf(100L));
        final BigDecimal expectedBudgetReserved = BigDecimal.valueOf(N*100);
        final BigDecimal expectedBudgetResidual = BigDecimal.valueOf(99);

        storeInitiative(budget);

        final ExecutorService executorService = Executors.newFixedThreadPool(N);
        LocalDateTime now = LocalDateTime.now();
        Long sequenceNumber = 123L;

        final List<Future<InitiativeCounters>> tasks = IntStream.range(0, N)
                .mapToObj(i -> executorService.submit(() ->
                        initiativeCountersReservationOpsRepositoryImpl.incrementOnboardedAndBudget("prova", "user"+i, budgetReservedPerRequestCents, sequenceNumber+i, now)))
                .toList();

        final long successfulReservation = tasks.stream().filter(t -> {
            try {
                return t.get() != null;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).count();

        checkStoredBudgetReservation(expectedBudgetReserved, expectedBudgetResidual, N);
        Assertions.assertEquals(N, successfulReservation);
    }

    private void storeInitiative(BigDecimal budget) {
        initiativeCountersRepository.save(InitiativeCounters.builder()
                .id("prova")
                .initiativeBudgetCents(euro2cents(budget))
                .residualInitiativeBudgetCents(euro2cents(budget))
                .build());
    }

    private long euro2cents(BigDecimal budget) {
        return budget.longValue() * 100;
    }

    private void checkStoredBudgetReservation(BigDecimal expectedBudgetReservedCents, BigDecimal expectedResidualBudgetCents, int expectedReservations) {
        final InitiativeCounters c = initiativeCountersRepository.findById("prova").orElseThrow(() -> new RuntimeException("test"));

        Assertions.assertNotNull(c);
        Assertions.assertEquals(euro2cents(expectedBudgetReservedCents), c.getReservedInitiativeBudgetCents());
        Assertions.assertEquals(euro2cents(expectedResidualBudgetCents), c.getResidualInitiativeBudgetCents());
        Assertions.assertEquals(expectedReservations, c.getOnboarded());
    }
}
