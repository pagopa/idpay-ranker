package it.gov.pagopa.ranker.service.ranker;

import java.util.Deque;
import java.util.Map;

public interface CycleHandlerState {
    boolean isCycleProcessing();
    boolean openCycle();
    void closeCycle();
    boolean hasFreeSlot(int maxParallelSessions);
    boolean isCompleted();
    Deque<String> getPendingSessions();
    Map<String, SessionWorker> getActiveSessions();

    boolean isInPending(String sessionId);
    boolean isInActive(String sessionId);
}
