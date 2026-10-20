package it.gov.pagopa.ranker.service.ranker;

import it.gov.pagopa.ranker.config.RankerProcessorProperties;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.ranker.service.ranker.sessionworker.NormalSessionWorker;
import it.gov.pagopa.ranker.service.ranker.sessionworker.SessionWorkerFactory;
import it.gov.pagopa.ranker.service.ranker.sessionworker.registry.SessionWorkerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CycleOrchestratorTest {

    @Mock
    private CycleHandlerState cycleHandlerStateMock;

    @Mock
    private Executor executorMock;

    @Mock
    private RankerProcessorProperties rankerProcessorPropertiesMock;

    @Mock
    private SessionWorkerFactory sessionWorkerFactoryMock;

    @Mock
    private InitiativeCountersService initiativeCountersServiceMock;

    @Mock
    private SessionWorkerRegistry sessionWorkerRegistryMock;

    private CycleOrchestrator cycleOrchestrator;

    @BeforeEach
    void setUp() {
        cycleOrchestrator = new CycleOrchestratorImpl(
                cycleHandlerStateMock,
                executorMock,
                rankerProcessorPropertiesMock,
                sessionWorkerFactoryMock,
                initiativeCountersServiceMock,
                sessionWorkerRegistryMock
        );
    }

    @Test
    void reconcile_whenNoInitiativesAvailable_shouldNotScheduleWorkers() {
        when(initiativeCountersServiceMock.retrieveInitiativesAvailableBudget())
                .thenReturn(List.of());

        cycleOrchestrator.reconcile();

        verify(initiativeCountersServiceMock)
                .retrieveInitiativesAvailableBudget();

        verifyNoInteractions(
                sessionWorkerFactoryMock,
                sessionWorkerRegistryMock,
                executorMock
        );
    }

    @Test
    void reconcile_whenCycleIsClosedAndInitiativeIsClaimed_shouldScheduleNormalWorker() {
        String initiativeId = "INITIATIVE_ID";
        ArrayDeque<String> pendingSessions = new ArrayDeque<>();
        HashMap<String, NormalSessionWorker> activeSessions = new HashMap<>();
        NormalSessionWorker workerMock = mock(NormalSessionWorker.class);

        when(initiativeCountersServiceMock.retrieveInitiativesAvailableBudget())
                .thenReturn(List.of(initiativeId));
        when(cycleHandlerStateMock.isCycleProcessing()).thenReturn(false);
        when(cycleHandlerStateMock.openCycle()).thenReturn(true);
        when(cycleHandlerStateMock.getPendingSessions()).thenReturn(pendingSessions);
        when(cycleHandlerStateMock.getActiveSessions()).thenReturn(activeSessions);
        when(rankerProcessorPropertiesMock.getMaxParallelSessions()).thenReturn(1);
        when(cycleHandlerStateMock.hasFreeSlot(1)).thenReturn(true);

        when(sessionWorkerFactoryMock.create(eq(initiativeId), any(Runnable.class)))
                .thenReturn(workerMock);
        when(sessionWorkerRegistryMock.tryClaimNormal(
                eq(initiativeId),
                any(UUID.class),
                same(workerMock)
        )).thenReturn(true);

        cycleOrchestrator.reconcile();

        verify(initiativeCountersServiceMock)
                .retrieveInitiativesAvailableBudget();
        verify(cycleHandlerStateMock).isCycleProcessing();
        verify(cycleHandlerStateMock).openCycle();

        verify(sessionWorkerFactoryMock)
                .create(eq(initiativeId), any(Runnable.class));

        verify(sessionWorkerRegistryMock)
                .tryClaimNormal(
                        eq(initiativeId),
                        any(UUID.class),
                        same(workerMock)
                );

        verify(executorMock).execute(workerMock);

        assertSame(workerMock, activeSessions.get(initiativeId));
        assertFalse(pendingSessions.contains(initiativeId));
    }

    @Test
    void reconcile_whenNormalClaimIsRejected_shouldNotScheduleWorker() {
        String initiativeId = "INITIATIVE_ID";
        ArrayDeque<String> pendingSessions = new ArrayDeque<>();
        HashMap<String, NormalSessionWorker> activeSessions = new HashMap<>();
        NormalSessionWorker workerMock = mock(NormalSessionWorker.class);

        when(initiativeCountersServiceMock.retrieveInitiativesAvailableBudget())
                .thenReturn(List.of(initiativeId));
        when(cycleHandlerStateMock.isCycleProcessing()).thenReturn(false);
        when(cycleHandlerStateMock.openCycle()).thenReturn(true);
        when(cycleHandlerStateMock.getPendingSessions()).thenReturn(pendingSessions);
        when(cycleHandlerStateMock.getActiveSessions()).thenReturn(activeSessions);
        when(rankerProcessorPropertiesMock.getMaxParallelSessions()).thenReturn(1);
        when(cycleHandlerStateMock.hasFreeSlot(1)).thenReturn(true);

        when(sessionWorkerFactoryMock.create(eq(initiativeId), any(Runnable.class)))
                .thenReturn(workerMock);
        when(sessionWorkerRegistryMock.tryClaimNormal(
                eq(initiativeId),
                any(UUID.class),
                same(workerMock)
        )).thenReturn(false);

        cycleOrchestrator.reconcile();

        verify(sessionWorkerRegistryMock)
                .tryClaimNormal(
                        eq(initiativeId),
                        any(UUID.class),
                        same(workerMock)
                );

        verify(executorMock, never()).execute(any(NormalSessionWorker.class));

        assertFalse(activeSessions.containsKey(initiativeId));
        assertFalse(pendingSessions.contains(initiativeId));
    }

    @Test
    void reconcile_whenExecutorRejectsWorker_shouldReleaseClaimAndRestorePendingInitiative() {
        String initiativeId = "INITIATIVE_ID";
        ArrayDeque<String> pendingSessions = new ArrayDeque<>();
        HashMap<String, NormalSessionWorker> activeSessions = new HashMap<>();
        NormalSessionWorker workerMock = mock(NormalSessionWorker.class);
        ArgumentCaptor<UUID> claimIdCaptor = ArgumentCaptor.forClass(UUID.class);

        when(initiativeCountersServiceMock.retrieveInitiativesAvailableBudget())
                .thenReturn(List.of(initiativeId));
        when(cycleHandlerStateMock.isCycleProcessing()).thenReturn(false);
        when(cycleHandlerStateMock.openCycle()).thenReturn(true);
        when(cycleHandlerStateMock.getPendingSessions()).thenReturn(pendingSessions);
        when(cycleHandlerStateMock.getActiveSessions()).thenReturn(activeSessions);
        when(rankerProcessorPropertiesMock.getMaxParallelSessions()).thenReturn(1);
        when(cycleHandlerStateMock.hasFreeSlot(1)).thenReturn(true);

        when(sessionWorkerFactoryMock.create(eq(initiativeId), any(Runnable.class)))
                .thenReturn(workerMock);
        when(sessionWorkerRegistryMock.tryClaimNormal(
                eq(initiativeId),
                any(UUID.class),
                same(workerMock)
        )).thenReturn(true);
        when(sessionWorkerRegistryMock.release(
                eq(initiativeId),
                any(UUID.class)
        )).thenReturn(true);

        doThrow(new RejectedExecutionException("Executor rejected worker"))
                .when(executorMock)
                .execute(workerMock);

        cycleOrchestrator.reconcile();

        verify(sessionWorkerRegistryMock).tryClaimNormal(
                eq(initiativeId),
                claimIdCaptor.capture(),
                same(workerMock)
        );

        verify(sessionWorkerRegistryMock).release(
                initiativeId,
                claimIdCaptor.getValue()
        );

        verify(executorMock).execute(workerMock);

        assertFalse(activeSessions.containsKey(initiativeId));
        assertEquals(initiativeId, pendingSessions.peekFirst());
    }

    @Test
    void reconcile_whenWorkerCompletes_shouldReleaseTheSameClaimIdAndRemoveActiveSession() {
        String initiativeId = "INITIATIVE_ID";
        ArrayDeque<String> pendingSessions = new ArrayDeque<>();
        HashMap<String, NormalSessionWorker> activeSessions = new HashMap<>();
        NormalSessionWorker workerMock = mock(NormalSessionWorker.class);

        ArgumentCaptor<Runnable> onCompletedCaptor =
                ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<UUID> claimIdCaptor =
                ArgumentCaptor.forClass(UUID.class);

        when(initiativeCountersServiceMock.retrieveInitiativesAvailableBudget())
                .thenReturn(List.of(initiativeId));
        when(cycleHandlerStateMock.isCycleProcessing()).thenReturn(false);
        when(cycleHandlerStateMock.openCycle()).thenReturn(true);
        when(cycleHandlerStateMock.getPendingSessions()).thenReturn(pendingSessions);
        when(cycleHandlerStateMock.getActiveSessions()).thenReturn(activeSessions);
        when(rankerProcessorPropertiesMock.getMaxParallelSessions()).thenReturn(1);
        when(cycleHandlerStateMock.hasFreeSlot(1)).thenReturn(true);
        when(cycleHandlerStateMock.isCompleted()).thenReturn(false, true);

        when(sessionWorkerFactoryMock.create(
                eq(initiativeId),
                any(Runnable.class)
        )).thenReturn(workerMock);

        when(sessionWorkerRegistryMock.tryClaimNormal(
                eq(initiativeId),
                any(UUID.class),
                same(workerMock)
        )).thenReturn(true);

        when(sessionWorkerRegistryMock.release(
                eq(initiativeId),
                any(UUID.class)
        )).thenReturn(true);

        cycleOrchestrator.reconcile();

        verify(sessionWorkerFactoryMock).create(
                eq(initiativeId),
                onCompletedCaptor.capture()
        );

        verify(sessionWorkerRegistryMock).tryClaimNormal(
                eq(initiativeId),
                claimIdCaptor.capture(),
                same(workerMock)
        );

        onCompletedCaptor.getValue().run();

        verify(sessionWorkerRegistryMock).release(
                initiativeId,
                claimIdCaptor.getValue()
        );

        assertFalse(activeSessions.containsKey(initiativeId));

        verify(cycleHandlerStateMock).closeCycle();
    }

    @Test
    void reconcile_whenCycleIsOpenAndAnotherInitiativeIsAvailable_shouldScheduleOnlyNewInitiative() {
        String initiativeId = "INITIATIVE_ID";
        String initiativeId2 = "INITIATIVE_ID_2";

        ArrayDeque<String> pendingSessions = new ArrayDeque<>();
        HashMap<String, NormalSessionWorker> activeSessions = new HashMap<>();

        NormalSessionWorker activeWorkerMock = mock(NormalSessionWorker.class);
        NormalSessionWorker newWorkerMock = mock(NormalSessionWorker.class);

        activeSessions.put(initiativeId, activeWorkerMock);

        when(initiativeCountersServiceMock.retrieveInitiativesAvailableBudget())
                .thenReturn(List.of(initiativeId, initiativeId2));
        when(cycleHandlerStateMock.isCycleProcessing()).thenReturn(true);
        when(cycleHandlerStateMock.getPendingSessions()).thenReturn(pendingSessions);
        when(cycleHandlerStateMock.getActiveSessions()).thenReturn(activeSessions);
        when(rankerProcessorPropertiesMock.getMaxParallelSessions()).thenReturn(2);
        when(cycleHandlerStateMock.hasFreeSlot(2)).thenReturn(true);

        when(sessionWorkerFactoryMock.create(eq(initiativeId2), any(Runnable.class)))
                .thenReturn(newWorkerMock);

        when(sessionWorkerRegistryMock.tryClaimNormal(
                eq(initiativeId2),
                any(UUID.class),
                same(newWorkerMock)
        )).thenReturn(true);

        cycleOrchestrator.reconcile();

        verify(initiativeCountersServiceMock)
                .retrieveInitiativesAvailableBudget();
        verify(cycleHandlerStateMock).isCycleProcessing();
        verify(cycleHandlerStateMock, times(3))
                .hasFreeSlot(2);

        verify(sessionWorkerFactoryMock)
                .create(eq(initiativeId2), any(Runnable.class));

        verify(sessionWorkerRegistryMock)
                .tryClaimNormal(
                        eq(initiativeId2),
                        any(UUID.class),
                        same(newWorkerMock)
                );

        verify(executorMock).execute(newWorkerMock);

        verify(sessionWorkerFactoryMock, never())
                .create(eq(initiativeId), any(Runnable.class));
    }
}
