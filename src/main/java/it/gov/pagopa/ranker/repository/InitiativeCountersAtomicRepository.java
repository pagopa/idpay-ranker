package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;

public interface InitiativeCountersAtomicRepository {
    InitiativeCounters incrementOnboardedAndBudget(String initiativeId, long reservationCents);
    InitiativeCounters decrementOnboardedAndBudget(String initiativeId, long reservationCents);
    InitiativeCounters updateCounterForCaptured(String initiativeId, Long effectiveAmountCents, Long voucherAmountCents);
    InitiativeCounters updateCounterForRefunded(String initiativeId, Long effectiveAmountCents);

    InitiativeCounters addSequenceToInitiative(String initiativeId, long sequenceNumber);

    InitiativeCounters removeUnprocessedSequenceId(String initativeId, Long sequenceId);
}
