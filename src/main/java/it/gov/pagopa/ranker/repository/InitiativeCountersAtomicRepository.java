package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import org.joda.time.DateTime;

public interface InitiativeCountersAtomicRepository {
    InitiativeCounters incrementOnboardedAndBudget(String initiativeId, String userId, long reservationCents, Long sequenceNumber, DateTime enqueuedTime);
}
