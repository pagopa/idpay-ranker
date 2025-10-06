package it.gov.pagopa.ranker.service.initative;

import org.joda.time.DateTime;

public interface InitiativeCountersService {
    void addPreallocatedUser(String initiativeId, String userId, boolean verifyIsee, Long sequenceNumber, DateTime enqueuedTime);
    boolean hasAvailableBudget();
}
