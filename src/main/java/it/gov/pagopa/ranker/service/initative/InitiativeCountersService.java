package it.gov.pagopa.ranker.service.initative;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.domain.model.InitiativeCountersPreallocations;

import java.time.LocalDateTime;
import java.util.Optional;

public interface InitiativeCountersService {
    void addPreallocatedUser(String initiativeId, String userId, boolean verifyIsee, Long sequenceNumber, LocalDateTime enqueuedTime);
    boolean hasAvailableBudget();
    boolean existsByInitiativeIdAndUserId(String initiativeId, String userId);
    void updateInitiativeCounters(TransactionInProgressDTO transactionInProgress, String preallocationId, String transactionInProgressId);
    Optional<InitiativeCountersPreallocations> findById(String initiativeId, String userId);
}
