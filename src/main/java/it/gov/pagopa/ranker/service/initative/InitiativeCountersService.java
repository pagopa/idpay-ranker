package it.gov.pagopa.ranker.service.initative;

import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDateTime;
import java.util.Set;

public interface InitiativeCountersService {
    void addPreallocatedUser(String initiativeId, String userId, boolean verifyIsee, Long sequenceNumber, LocalDateTime enqueuedTime);
    boolean hasAvailableBudget();
    boolean existsByInitiativeIdAndUserId(String initiativeId, String userId);
    void removeMessageToProcessOnInitative(String initativeId, Long sequenceId);
    Set<Pair<String,Long>> getMessageToProcess();

    void addMessageProcessOnInitiative(long sequenceNumber, String initiativeId);
}
