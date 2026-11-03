package it.gov.pagopa.ranker.service.ranker;

import it.gov.pagopa.ranker.service.ranker.sessionworker.NormalSessionWorker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Deque;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class CycleHandlerStateTest {
    private CycleHandlerState cycleHandlerState;

    @BeforeEach
    void setUp() {
        cycleHandlerState = new CycleHandlerStateImpl();
    }

    @Test
    void isCycleProcessing(){
        boolean result = cycleHandlerState.isCycleProcessing();

        Assertions.assertFalse(result);
    }

    @Test
    void openCycle(){
        boolean initialFalse = cycleHandlerState.isCycleProcessing();
        Assertions.assertFalse(initialFalse);

        boolean result = cycleHandlerState.openCycle();
        Assertions.assertTrue(result);
    }

    @Test
    void closeCycle(){
        cycleHandlerState.openCycle();
        cycleHandlerState.getPendingSessions().add("TEST");

        cycleHandlerState.closeCycle();
        Assertions.assertFalse(cycleHandlerState.isCycleProcessing());
        Assertions.assertTrue(cycleHandlerState.getPendingSessions().isEmpty());
    }

    @Test
    void getPendingSessions(){
        Deque<String> result1 = cycleHandlerState.getPendingSessions();
        Assertions.assertNotNull(result1);
        Assertions.assertTrue(result1.isEmpty());

        result1.add("TEST");
        Deque<String> result2 = cycleHandlerState.getPendingSessions();
        Assertions.assertNotNull(result2);
        Assertions.assertEquals(1, result2.size());
    }

    @Test
    void getActiveSessions(){
        Map<String, NormalSessionWorker> result1 = cycleHandlerState.getActiveSessions();
        Assertions.assertNotNull(result1);
        Assertions.assertTrue(result1.isEmpty());

        NormalSessionWorker sessionWorkerMock = Mockito.mock(NormalSessionWorker.class);
        result1.put("TEST", sessionWorkerMock);
        Map<String, NormalSessionWorker> result2 = cycleHandlerState.getActiveSessions();
        Assertions.assertNotNull(result2);
        Assertions.assertEquals(1, result2.size());
    }

    @Test
    void isInPending_isNotPresent(){
        boolean result = cycleHandlerState.isInPending("TEST");
        Assertions.assertFalse(result);
    }

    @Test
    void isInPending_isPresent(){
        String sessionId = "TEST";
        cycleHandlerState.getPendingSessions().add(sessionId);

        boolean result = cycleHandlerState.isInPending(sessionId);
        Assertions.assertTrue(result);
    }

    @Test
    void isInActive_isNotPresent(){
        boolean result = cycleHandlerState.isInActive("TEST");
        Assertions.assertFalse(result);
    }

    @Test
    void isInActive_isPresent(){
        String sessionId = "TEST";
        NormalSessionWorker sessionWorkerMock = Mockito.mock(NormalSessionWorker.class);
        cycleHandlerState.getActiveSessions().put(sessionId, sessionWorkerMock);

        boolean result = cycleHandlerState.isInActive(sessionId);
        Assertions.assertTrue(result);
    }

    @Test
    void hasFreeSlot_true(){
        boolean result = cycleHandlerState.hasFreeSlot(1);
        Assertions.assertTrue(result);
    }

    @Test
    void hasFreeSlot_false(){
        String sessionId = "TEST";
        NormalSessionWorker sessionWorkerMock = Mockito.mock(NormalSessionWorker.class);
        cycleHandlerState.getActiveSessions().put(sessionId, sessionWorkerMock);

        boolean result = cycleHandlerState.hasFreeSlot(1);
        Assertions.assertFalse(result);
    }

    @Test
    void isCompleted_true(){
        boolean result = cycleHandlerState.isCompleted();
        Assertions.assertTrue(result);
    }

    @Test
    void isCompleted_withPendingPresent(){
        String sessionId = "TEST";
        cycleHandlerState.getPendingSessions().add(sessionId);

        boolean result = cycleHandlerState.isCompleted();
        Assertions.assertFalse(result);
    }

    @Test
    void isCompleted_withActivePresent(){
        String sessionId = "TEST";
        NormalSessionWorker sessionWorkerMock = Mockito.mock(NormalSessionWorker.class);
        cycleHandlerState.getActiveSessions().put(sessionId, sessionWorkerMock);

        boolean result = cycleHandlerState.isCompleted();
        Assertions.assertFalse(result);
    }

    @Test
    void isCompleted_withPendingActivePresent(){
        cycleHandlerState.getPendingSessions().add("TEST1");
        NormalSessionWorker sessionWorkerMock = Mockito.mock(NormalSessionWorker.class);
        cycleHandlerState.getActiveSessions().put("TEST2", sessionWorkerMock);

        boolean result = cycleHandlerState.isCompleted();
        Assertions.assertFalse(result);
    }
}