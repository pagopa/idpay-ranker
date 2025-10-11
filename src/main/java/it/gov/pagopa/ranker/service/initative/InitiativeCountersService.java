package it.gov.pagopa.ranker.service.initative;

import java.time.LocalDateTime;

public interface InitiativeCountersService {
    void addPreallocatedUser(String initiativeId, String userId, boolean verifyIsee, Long sequenceNumber, LocalDateTime enqueuedTime);
    boolean hasAvailableBudget();
    boolean existsByInitiativeIdAndUserId(String initiativeId, String userId);
}
