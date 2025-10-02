package it.gov.pagopa.ranker.service.ranker;

import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RankerConsumerScheduler {

    private final InitiativeCountersRepository initiativeCountersRepository;
    private final RankerServiceImpl rankerService;

    public RankerConsumerScheduler(InitiativeCountersRepository initiativeCountersRepository,
                                   RankerServiceImpl rankerService) {
        this.initiativeCountersRepository = initiativeCountersRepository;
        this.rankerService = rankerService;
    }

    @Scheduled(fixedRate = 300_000)
    public void checkResidualBudgetAndStartConsumer() {
        initiativeCountersRepository.findByResidualBudgetGreaterThanEqual(100L)
                .forEach(counters -> rankerService.startConsumer());
    }
}
