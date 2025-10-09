package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;

public interface InitiativeCountersAtomicRepository {
    InitiativeCounters incrementOnboardedAndBudget(String initiativeId, String userId, long reservationCents);
    InitiativeCounters decrementOnboardedAndBudget(String initiativeId, String userId, long reservationCents);

}
