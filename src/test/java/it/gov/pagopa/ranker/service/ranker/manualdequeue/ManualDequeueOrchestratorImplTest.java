package it.gov.pagopa.ranker.service.ranker.manualdequeue;

import it.gov.pagopa.ranker.service.ranker.CycleHandlerState;
import it.gov.pagopa.ranker.service.ranker.SessionWorkerFactory;
import it.gov.pagopa.ranker.service.ranker.sessionworker.ManualOnlySessionWorker;
import it.gov.pagopa.ranker.service.ranker.sessionworker.NormalSessionWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualDequeueOrchestratorImplTest {

    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final String TRACE_ID = "TRACE_ID";
    private static final int REQUESTED_MESSAGES = 10;

    @Mock
    private CycleHandlerState cycleHandlerStateMock;

    @Mock
    private SessionWorkerFactory sessionWorkerFactoryMock;

    @Mock
    private Runnable onCompletedMock;

    private ManualDequeueOrchestratorImpl manualDequeueOrchestrator;
    private ManualDequeueRequest manualDequeueRequest;

    @BeforeEach
    void setUp() {
        manualDequeueOrchestrator = new ManualDequeueOrchestratorImpl(
                cycleHandlerStateMock,
                sessionWorkerFactoryMock
        );

        manualDequeueRequest = new ManualDequeueRequest(
                INITIATIVE_ID,
                REQUESTED_MESSAGES,
                TRACE_ID,
                onCompletedMock
        );
    }

    @Test
    void orchestrate_withActiveNormalSessionWorkerAndAcceptedRequest_queuesRequest() {
        NormalSessionWorker activeWorkerMock =
                mock(NormalSessionWorker.class);

        Map<String, NormalSessionWorker> activeSessions =
                new HashMap<>();

        activeSessions.put(INITIATIVE_ID, activeWorkerMock);

        when(cycleHandlerStateMock.getActiveSessions())
                .thenReturn(activeSessions);

        when(activeWorkerMock.tryEnqueueManualRequest(manualDequeueRequest))
                .thenReturn(true);

        manualDequeueOrchestrator.orchestrate(manualDequeueRequest);

        verify(activeWorkerMock)
                .tryEnqueueManualRequest(manualDequeueRequest);

        verifyNoInteractions(
                sessionWorkerFactoryMock,
                onCompletedMock
        );
    }

    @Test
    void orchestrate_withActiveNormalSessionWorkerAndRejectedRequest_releasesOwnership() {
        NormalSessionWorker activeWorkerMock =
                mock(NormalSessionWorker.class);

        Map<String, NormalSessionWorker> activeSessions =
                new HashMap<>();

        activeSessions.put(INITIATIVE_ID, activeWorkerMock);

        when(cycleHandlerStateMock.getActiveSessions())
                .thenReturn(activeSessions);

        when(activeWorkerMock.tryEnqueueManualRequest(manualDequeueRequest))
                .thenReturn(false);

        manualDequeueOrchestrator.orchestrate(manualDequeueRequest);

        verify(activeWorkerMock)
                .tryEnqueueManualRequest(manualDequeueRequest);

        verify(onCompletedMock).run();

        verifyNoInteractions(sessionWorkerFactoryMock);
    }

    @Test
    void orchestrate_withoutActiveNormalSessionWorker_createsAndRunsManualOnlyWorker() {
        ManualOnlySessionWorker manualOnlyWorkerMock =
                mock(ManualOnlySessionWorker.class);

        when(cycleHandlerStateMock.getActiveSessions())
                .thenReturn(Collections.emptyMap());

        when(
                sessionWorkerFactoryMock.createManualOnly(
                        INITIATIVE_ID,
                        REQUESTED_MESSAGES,
                        onCompletedMock
                )
        ).thenReturn(manualOnlyWorkerMock);

        manualDequeueOrchestrator.orchestrate(manualDequeueRequest);

        verify(sessionWorkerFactoryMock).createManualOnly(
                INITIATIVE_ID,
                REQUESTED_MESSAGES,
                onCompletedMock
        );

        verify(manualOnlyWorkerMock).run();

        /*
         * The ManualOnlySessionWorker owns the request now.
         * It is responsible for invoking onCompleted() in its lifecycle.
         */
        verifyNoInteractions(onCompletedMock);
    }

    @Test
    void orchestrate_whenManualOnlyWorkerFactoryFails_releasesOwnership() {
        when(cycleHandlerStateMock.getActiveSessions())
                .thenReturn(Collections.emptyMap());

        when(
                sessionWorkerFactoryMock.createManualOnly(
                        INITIATIVE_ID,
                        REQUESTED_MESSAGES,
                        onCompletedMock
                )
        ).thenThrow(new RuntimeException("Factory error"));

        manualDequeueOrchestrator.orchestrate(manualDequeueRequest);

        verify(onCompletedMock).run();
    }

    @Test
    void orchestrate_whenManualOnlyWorkerRunFails_releasesOwnership() {
        ManualOnlySessionWorker manualOnlyWorkerMock =
                mock(ManualOnlySessionWorker.class);

        when(cycleHandlerStateMock.getActiveSessions())
                .thenReturn(Collections.emptyMap());

        when(
                sessionWorkerFactoryMock.createManualOnly(
                        INITIATIVE_ID,
                        REQUESTED_MESSAGES,
                        onCompletedMock
                )
        ).thenReturn(manualOnlyWorkerMock);

        doThrow(new RuntimeException("Worker run error"))
                .when(manualOnlyWorkerMock)
                .run();

        manualDequeueOrchestrator.orchestrate(manualDequeueRequest);

        verify(manualOnlyWorkerMock).run();
        verify(onCompletedMock).run();
    }

    @Test
    void orchestrate_whenCycleHandlerStateFails_releasesOwnership() {
        when(cycleHandlerStateMock.getActiveSessions())
                .thenThrow(new RuntimeException("State error"));

        manualDequeueOrchestrator.orchestrate(manualDequeueRequest);

        verify(onCompletedMock).run();

        verifyNoInteractions(sessionWorkerFactoryMock);
    }
}
