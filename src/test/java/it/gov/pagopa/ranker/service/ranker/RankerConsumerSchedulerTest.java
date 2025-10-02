package it.gov.pagopa.ranker.service.ranker;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

class RankerConsumerSchedulerTest {

    private InitiativeCountersRepository initiativeCountersRepository;
    private RankerServiceImpl rankerService;
    private RankerConsumerScheduler scheduler;

    @BeforeEach
    void setUp() {
        initiativeCountersRepository = mock(InitiativeCountersRepository.class);
        rankerService = mock(RankerServiceImpl.class);
        scheduler = new RankerConsumerScheduler(initiativeCountersRepository, rankerService);
    }

    @Test
    void testSchedulerInvokesStartConsumerForHighBudget() {
        InitiativeCounters counters1 = InitiativeCounters.builder().id("initiative1").build();
        InitiativeCounters counters2 = InitiativeCounters.builder().id("initiative2").build();

        when(initiativeCountersRepository.findByResidualBudgetGreaterThanEqual(100L))
                .thenReturn(Arrays.asList(counters1, counters2));

        scheduler.checkResidualBudgetAndStartConsumer();

        verify(rankerService, times(2)).startConsumer();
        verifyNoMoreInteractions(rankerService);
    }

    @Test
    void testSchedulerDoesNothingWhenNoHighBudgetCounters() {
        when(initiativeCountersRepository.findByResidualBudgetGreaterThanEqual(100L))
                .thenReturn(Collections.emptyList());

        scheduler.checkResidualBudgetAndStartConsumer();

        verifyNoInteractions(rankerService);
    }
}
