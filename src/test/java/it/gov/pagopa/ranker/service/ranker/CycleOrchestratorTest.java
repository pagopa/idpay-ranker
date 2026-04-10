package it.gov.pagopa.ranker.service.ranker;

import it.gov.pagopa.ranker.config.RankerProcessorProperties;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;

import static org.mockito.Mockito.*;

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

    private CycleOrchestrator cycleOrchestrator;

    @BeforeEach
    void setUp() {
        cycleOrchestrator = new CycleOrchestratorImpl(cycleHandlerStateMock,
                executorMock,
                rankerProcessorPropertiesMock,
                sessionWorkerFactoryMock,
                initiativeCountersServiceMock);
    }

    @Test
    void reconcile_initialCycleStatusCloseAndInitiativesNotAvailablesBudget() {
        when(initiativeCountersServiceMock.retrieveInitiativesAvailableBudget()).thenReturn(List.of());

        cycleOrchestrator.reconcile();

        verify(initiativeCountersServiceMock).retrieveInitiativesAvailableBudget();
    }

    @Test
    void reconcile_initialCycleStatusCloseAndInitiativesAvailablesBudget() {
        String initiative = "INITIATIVE_ID";
        when(initiativeCountersServiceMock.retrieveInitiativesAvailableBudget()).thenReturn(List.of(initiative));

        when(cycleHandlerStateMock.isCycleProcessing()).thenReturn(false);

        when(cycleHandlerStateMock.openCycle()).thenReturn(true);
        ArrayDeque<String> deque = new ArrayDeque<>();
        when(cycleHandlerStateMock.getPendingSessions()).thenReturn(deque);

        when(rankerProcessorPropertiesMock.getMaxParallelSessions()).thenReturn(1);
        when(cycleHandlerStateMock.hasFreeSlot(1)).thenReturn(true);

        HashMap<String, SessionWorker> activeMock = new HashMap<>();
        when(cycleHandlerStateMock.getActiveSessions()).thenReturn(activeMock);

        SessionWorker sessionWorkerMock = mock(SessionWorker.class);
        when(sessionWorkerFactoryMock.create(eq(initiative), any())).thenReturn(sessionWorkerMock);

        doNothing().when(executorMock).execute(sessionWorkerMock);

        cycleOrchestrator.reconcile();

        verify(initiativeCountersServiceMock).retrieveInitiativesAvailableBudget();
        verify(cycleHandlerStateMock).isCycleProcessing();
        verify(cycleHandlerStateMock).openCycle();
        verify(sessionWorkerFactoryMock).create(anyString(),any());
    }

    @Test
    void reconcile_initiativeCycleStatusOpenAndAnotherInitiativeAvailableBudget(){
        String initiative = "INITIATIVE_ID";
        String initiative2 = "INITIATIVE_ID_2";
        when(initiativeCountersServiceMock.retrieveInitiativesAvailableBudget()).thenReturn(List.of(initiative, initiative2));

        when(cycleHandlerStateMock.isCycleProcessing()).thenReturn(true);

        ArrayDeque<String> deque = new ArrayDeque<>();
        when(cycleHandlerStateMock.getPendingSessions()).thenReturn(deque);

        when(rankerProcessorPropertiesMock.getMaxParallelSessions()).thenReturn(2);
        when(cycleHandlerStateMock.hasFreeSlot(2)).thenReturn(true);

        HashMap<String, SessionWorker> activeMock = new HashMap<>();
        SessionWorker sessionWorkerMock = mock(SessionWorker.class);
        activeMock.put(initiative, sessionWorkerMock);
        when(cycleHandlerStateMock.getActiveSessions()).thenReturn(activeMock);

        SessionWorker sessionWorkerMock2 = mock(SessionWorker.class);
        when(sessionWorkerFactoryMock.create(eq(initiative2), any())).thenReturn(sessionWorkerMock2);

        doNothing().when(executorMock).execute(sessionWorkerMock2);

        cycleOrchestrator.reconcile();

        verify(initiativeCountersServiceMock).retrieveInitiativesAvailableBudget();
        verify(cycleHandlerStateMock).isCycleProcessing();
        verify(cycleHandlerStateMock, times(3)).hasFreeSlot(anyInt());
        verify(sessionWorkerFactoryMock).create(anyString(),any());
    }
}