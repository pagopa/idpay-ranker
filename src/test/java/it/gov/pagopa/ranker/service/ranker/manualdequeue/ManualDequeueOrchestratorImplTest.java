package it.gov.pagopa.ranker.service.ranker.manualdequeue;

import it.gov.pagopa.ranker.service.ranker.sessionworker.ManualOnlySessionWorker;
import it.gov.pagopa.ranker.service.ranker.sessionworker.NormalSessionWorker;
import it.gov.pagopa.ranker.service.ranker.sessionworker.SessionWorkerFactory;
import it.gov.pagopa.ranker.enums.ManualClaimOutcome;
import it.gov.pagopa.ranker.service.ranker.sessionworker.registry.ManualClaimResult;
import it.gov.pagopa.ranker.service.ranker.sessionworker.registry.SessionWorkerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualDequeueOrchestratorImplTest {

    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final String TRACE_ID = "TRACE_ID";
    private static final int REQUESTED_MESSAGES = 10;

    @Mock
    private SessionWorkerFactory sessionWorkerFactoryMock;

    @Mock
    private SessionWorkerRegistry sessionWorkerRegistryMock;

    private ManualDequeueOrchestratorImpl manualDequeueOrchestrator;
    private ManualDequeueRequest manualDequeueRequest;

    @BeforeEach
    void setUp() {
        manualDequeueOrchestrator = new ManualDequeueOrchestratorImpl(
                sessionWorkerFactoryMock,
                sessionWorkerRegistryMock
        );

        manualDequeueRequest = new ManualDequeueRequest(
                INITIATIVE_ID,
                REQUESTED_MESSAGES,
                TRACE_ID
        );
    }

    @Test
    void orchestrate_whenSessionIsOwnedByNormalWorkerAndRequestIsAccepted_shouldQueueRequest() {
        NormalSessionWorker normalWorkerMock = mock(NormalSessionWorker.class);

        when(sessionWorkerRegistryMock.tryClaimManual(
                eq(INITIATIVE_ID),
                any(UUID.class)
        )).thenReturn(ManualClaimResult.ownedByNormal(normalWorkerMock));

        when(normalWorkerMock.tryEnqueueManualRequest(manualDequeueRequest))
                .thenReturn(true);

        manualDequeueOrchestrator.orchestrate(manualDequeueRequest);

        verify(sessionWorkerRegistryMock).tryClaimManual(
                eq(INITIATIVE_ID),
                any(UUID.class)
        );

        verify(normalWorkerMock)
                .tryEnqueueManualRequest(manualDequeueRequest);

        verifyNoInteractions(sessionWorkerFactoryMock);

        verify(sessionWorkerRegistryMock, never()).release(
                eq(INITIATIVE_ID),
                any(UUID.class)
        );
    }

    @Test
    void orchestrate_whenSessionIsOwnedByNormalWorkerAndRequestIsRejected_shouldNotCreateManualOnlyWorker() {
        NormalSessionWorker normalWorkerMock = mock(NormalSessionWorker.class);

        when(sessionWorkerRegistryMock.tryClaimManual(
                eq(INITIATIVE_ID),
                any(UUID.class)
        )).thenReturn(ManualClaimResult.ownedByNormal(normalWorkerMock));

        when(normalWorkerMock.tryEnqueueManualRequest(manualDequeueRequest))
                .thenReturn(false);

        manualDequeueOrchestrator.orchestrate(manualDequeueRequest);

        verify(normalWorkerMock)
                .tryEnqueueManualRequest(manualDequeueRequest);

        verifyNoInteractions(sessionWorkerFactoryMock);

        verify(sessionWorkerRegistryMock, never()).release(
                eq(INITIATIVE_ID),
                any(UUID.class)
        );
    }

    @Test
    void orchestrate_whenManualClaimIsAcquired_shouldCreateRunAndReleaseManualOnlyWorker() {
        ManualOnlySessionWorker manualOnlyWorkerMock =
                mock(ManualOnlySessionWorker.class);

        ArgumentCaptor<UUID> claimIdCaptor =
                ArgumentCaptor.forClass(UUID.class);

        when(sessionWorkerRegistryMock.tryClaimManual(
                eq(INITIATIVE_ID),
                any(UUID.class)
        )).thenReturn(ManualClaimResult.claimed());

        when(sessionWorkerFactoryMock.createManualOnly(
                INITIATIVE_ID,
                REQUESTED_MESSAGES
        )).thenReturn(manualOnlyWorkerMock);

        when(sessionWorkerRegistryMock.release(
                eq(INITIATIVE_ID),
                any(UUID.class)
        )).thenReturn(true);

        manualDequeueOrchestrator.orchestrate(manualDequeueRequest);

        verify(sessionWorkerRegistryMock).tryClaimManual(
                eq(INITIATIVE_ID),
                claimIdCaptor.capture()
        );

        verify(sessionWorkerFactoryMock).createManualOnly(
                INITIATIVE_ID,
                REQUESTED_MESSAGES
        );

        verify(manualOnlyWorkerMock).run();

        verify(sessionWorkerRegistryMock).release(
                INITIATIVE_ID,
                claimIdCaptor.getValue()
        );
    }

    @Test
    void orchestrate_whenManualOnlyWorkerFactoryFails_shouldStillReleaseManualClaim() {
        ArgumentCaptor<UUID> claimIdCaptor =
                ArgumentCaptor.forClass(UUID.class);

        when(sessionWorkerRegistryMock.tryClaimManual(
                eq(INITIATIVE_ID),
                any(UUID.class)
        )).thenReturn(ManualClaimResult.claimed());

        when(sessionWorkerFactoryMock.createManualOnly(
                INITIATIVE_ID,
                REQUESTED_MESSAGES
        )).thenThrow(new RuntimeException("Factory error"));

        when(sessionWorkerRegistryMock.release(
                eq(INITIATIVE_ID),
                any(UUID.class)
        )).thenReturn(true);

        assertDoesNotThrow(() ->
                manualDequeueOrchestrator.orchestrate(manualDequeueRequest)
        );

        verify(sessionWorkerRegistryMock).tryClaimManual(
                eq(INITIATIVE_ID),
                claimIdCaptor.capture()
        );

        verify(sessionWorkerFactoryMock).createManualOnly(
                INITIATIVE_ID,
                REQUESTED_MESSAGES
        );

        verify(sessionWorkerRegistryMock).release(
                INITIATIVE_ID,
                claimIdCaptor.getValue()
        );
    }

    @Test
    void orchestrate_whenManualOnlyWorkerRunFails_shouldStillReleaseManualClaim() {
        ManualOnlySessionWorker manualOnlyWorkerMock =
                mock(ManualOnlySessionWorker.class);

        ArgumentCaptor<UUID> claimIdCaptor =
                ArgumentCaptor.forClass(UUID.class);

        when(sessionWorkerRegistryMock.tryClaimManual(
                eq(INITIATIVE_ID),
                any(UUID.class)
        )).thenReturn(ManualClaimResult.claimed());

        when(sessionWorkerFactoryMock.createManualOnly(
                INITIATIVE_ID,
                REQUESTED_MESSAGES
        )).thenReturn(manualOnlyWorkerMock);

        doThrow(new RuntimeException("Worker run error"))
                .when(manualOnlyWorkerMock)
                .run();

        when(sessionWorkerRegistryMock.release(
                eq(INITIATIVE_ID),
                any(UUID.class)
        )).thenReturn(true);

        assertDoesNotThrow(() ->
                manualDequeueOrchestrator.orchestrate(manualDequeueRequest)
        );

        verify(sessionWorkerRegistryMock).tryClaimManual(
                eq(INITIATIVE_ID),
                claimIdCaptor.capture()
        );

        verify(sessionWorkerFactoryMock).createManualOnly(
                INITIATIVE_ID,
                REQUESTED_MESSAGES
        );

        verify(manualOnlyWorkerMock).run();

        verify(sessionWorkerRegistryMock).release(
                INITIATIVE_ID,
                claimIdCaptor.getValue()
        );
    }

    @Test
    void orchestrate_whenSessionIsAlreadyOwnedByManualWorker_shouldNotCreateOrReleaseWorker() {
        when(sessionWorkerRegistryMock.tryClaimManual(
                eq(INITIATIVE_ID),
                any(UUID.class)
        )).thenReturn(ManualClaimResult.ownedByManual());

        manualDequeueOrchestrator.orchestrate(manualDequeueRequest);

        verify(sessionWorkerRegistryMock).tryClaimManual(
                eq(INITIATIVE_ID),
                any(UUID.class)
        );

        verifyNoInteractions(sessionWorkerFactoryMock);

        verify(sessionWorkerRegistryMock, never()).release(
                eq(INITIATIVE_ID),
                any(UUID.class)
        );
    }

    @Test
    void orchestrate_whenManualClaimRegistryFails_shouldNotCreateManualOnlyWorker() {
        when(sessionWorkerRegistryMock.tryClaimManual(
                eq(INITIATIVE_ID),
                any(UUID.class)
        )).thenThrow(new RuntimeException("Registry error"));

        assertDoesNotThrow(() ->
                manualDequeueOrchestrator.orchestrate(manualDequeueRequest)
        );

        verifyNoInteractions(sessionWorkerFactoryMock);

        verify(sessionWorkerRegistryMock, never()).release(
                eq(INITIATIVE_ID),
                any(UUID.class)
        );
    }

    @Test
    void orchestrate_whenManualClaimReleaseFails_shouldNotPropagateException() {
        ManualOnlySessionWorker manualOnlyWorkerMock =
                mock(ManualOnlySessionWorker.class);

        when(sessionWorkerRegistryMock.tryClaimManual(
                eq(INITIATIVE_ID),
                any(UUID.class)
        )).thenReturn(ManualClaimResult.claimed());

        when(sessionWorkerFactoryMock.createManualOnly(
                INITIATIVE_ID,
                REQUESTED_MESSAGES
        )).thenReturn(manualOnlyWorkerMock);

        when(sessionWorkerRegistryMock.release(
                eq(INITIATIVE_ID),
                any(UUID.class)
        )).thenThrow(new RuntimeException("Release error"));

        assertDoesNotThrow(() ->
                manualDequeueOrchestrator.orchestrate(manualDequeueRequest)
        );

        verify(manualOnlyWorkerMock).run();

        verify(sessionWorkerRegistryMock).release(
                eq(INITIATIVE_ID),
                any(UUID.class)
        );
    }

    @Test
    void orchestrate_whenRegistryReturnsOwnedByNormalWithoutWorker_shouldNotCreateManualOnlyWorker() {
        ManualClaimResult manualClaimResultMock =
                mock(ManualClaimResult.class);

        when(manualClaimResultMock.outcome())
                .thenReturn(ManualClaimOutcome.OWNED_BY_NORMAL);

        when(manualClaimResultMock.normalWorker())
                .thenReturn(null);

        when(sessionWorkerRegistryMock.tryClaimManual(
                eq(INITIATIVE_ID),
                any(UUID.class)
        )).thenReturn(manualClaimResultMock);

        assertDoesNotThrow(() ->
                manualDequeueOrchestrator.orchestrate(manualDequeueRequest)
        );

        verifyNoInteractions(sessionWorkerFactoryMock);

        verify(sessionWorkerRegistryMock, never()).release(
                eq(INITIATIVE_ID),
                any(UUID.class)
        );
    }
}
