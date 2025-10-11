package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;

import java.time.LocalDateTime;

public interface InitiativeCountersAtomicRepository {
    InitiativeCounters incrementOnboardedAndBudget(String initiativeId, long reservationCents);
    InitiativeCounters decrementOnboardedAndBudget(String initiativeId, String userId, long reservationCents);
}
