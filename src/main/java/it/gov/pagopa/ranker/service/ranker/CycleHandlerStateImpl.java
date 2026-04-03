package it.gov.pagopa.ranker.service.ranker;

import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CycleHandlerStateImpl implements CycleHandlerState{
    private final AtomicBoolean cycleProcessing = new AtomicBoolean(false);
    private final Deque<String> pendingSessions = new ConcurrentLinkedDeque<>();
   private final Map<String, SessionWorker> activeSessions = new ConcurrentHashMap<>();

    @Override
    public boolean isCycleProcessing() {
        return cycleProcessing.get();
    }

    @Override
    public boolean openCycle() {
        return cycleProcessing.compareAndSet(false, true);
    }

    @Override
    public void closeCycle() {
        cycleProcessing.set(false);
        pendingSessions.clear();
        //todo active?
    }

    @Override
    public Deque<String> getPendingSessions() {
        return pendingSessions;
    }

    @Override
    public Map<String, SessionWorker> getActiveSessions() {
        return activeSessions;
    }

    @Override
    public boolean isInPending(String sessionId) {
        return pendingSessions.contains(sessionId);
    }

    @Override
    public boolean isInActive(String sessionId) {
        return activeSessions.containsKey(sessionId);
    }

    @Override
    public boolean hasFreeSlot(int maxParallelSessions) {
        return activeSessions.size() < maxParallelSessions;
    }

    @Override
    public boolean isCompleted() {
        return pendingSessions.isEmpty() && activeSessions.isEmpty();
    }


}
