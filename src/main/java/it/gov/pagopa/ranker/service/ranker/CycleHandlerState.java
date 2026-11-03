package it.gov.pagopa.ranker.service.ranker;

import it.gov.pagopa.ranker.service.ranker.sessionworker.NormalSessionWorker;

import java.util.Deque;
import java.util.Map;

public interface CycleHandlerState {
    boolean isCycleProcessing();
    boolean openCycle();
    void closeCycle();
    boolean hasFreeSlot(int maxParallelSessions);
    boolean isCompleted();
    Deque<String> getPendingSessions();
    Map<String, NormalSessionWorker> getActiveSessions();

    boolean isInPending(String sessionId);
    boolean isInActive(String sessionId);
}
