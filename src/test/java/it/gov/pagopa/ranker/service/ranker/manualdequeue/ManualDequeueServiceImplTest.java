package it.gov.pagopa.ranker.service.ranker.manualdequeue;

import it.gov.pagopa.ranker.domain.dto.ManualDequeueResponseDTO;
import it.gov.pagopa.ranker.exception.ManualDequeueSizeNotValidException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.concurrent.Executor;

import static it.gov.pagopa.ranker.constants.RankerConstants.MDC_TRACE_ID;
import static it.gov.pagopa.ranker.constants.RankerConstants.STATUS_ACCEPTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ManualDequeueServiceImplTest {

    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final String TRACE_ID = "TRACE_ID";
    private static final int REQUESTED_MESSAGES = 10;

    @Mock
    private Executor manualDequeueOrchestratorExecutorMock;

    @Mock
    private ManualDequeueOrchestrator manualDequeueOrchestratorMock;

    private ManualDequeueServiceImpl manualDequeueService;

    @BeforeEach
    void setUp() {
        manualDequeueService = new ManualDequeueServiceImpl(
                manualDequeueOrchestratorExecutorMock,
                manualDequeueOrchestratorMock
        );
    }

    @AfterEach
    void tearDown() {
        MDC.remove(MDC_TRACE_ID);
    }

    @Test
    void requestDequeue_withNonPositiveSize_throwsException() {
        Assertions.assertThrows(
                ManualDequeueSizeNotValidException.class,
                () -> manualDequeueService.requestDequeue(
                        INITIATIVE_ID,
                        0
                )
        );

        verifyNoInteractions(
                manualDequeueOrchestratorExecutorMock,
                manualDequeueOrchestratorMock
        );
    }

    @Test
    void requestDequeue_validRequest_returnsAcceptedResponseAndOrchestratesRequest() {
        MDC.put(MDC_TRACE_ID, TRACE_ID);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();

            return null;
        }).when(manualDequeueOrchestratorExecutorMock).execute(any(Runnable.class));

        ManualDequeueResponseDTO response = manualDequeueService.requestDequeue(
                INITIATIVE_ID,
                REQUESTED_MESSAGES
        );

        Assertions.assertEquals(INITIATIVE_ID, response.getInitiativeId());
        Assertions.assertEquals(REQUESTED_MESSAGES, response.getRequestedMessages());
        Assertions.assertEquals(STATUS_ACCEPTED, response.getStatus());
        Assertions.assertEquals(TRACE_ID, response.getTraceId());

        ArgumentCaptor<ManualDequeueRequest> requestCaptor =
                ArgumentCaptor.forClass(ManualDequeueRequest.class);

        verify(manualDequeueOrchestratorMock)
                .orchestrate(requestCaptor.capture());

        ManualDequeueRequest capturedRequest = requestCaptor.getValue();

        Assertions.assertEquals(INITIATIVE_ID, capturedRequest.initiativeId());
        Assertions.assertEquals(REQUESTED_MESSAGES, capturedRequest.size());
        Assertions.assertEquals(TRACE_ID, capturedRequest.traceId());
    }

    @Test
    void requestDequeue_whenInitiativeIsAlreadyOwned_doesNotSubmitAnotherTask() {
        manualDequeueService.requestDequeue(
                INITIATIVE_ID,
                REQUESTED_MESSAGES
        );

        ManualDequeueResponseDTO secondResponse =
                manualDequeueService.requestDequeue(
                        INITIATIVE_ID,
                        REQUESTED_MESSAGES
                );

        Assertions.assertEquals(INITIATIVE_ID, secondResponse.getInitiativeId());
        Assertions.assertEquals(REQUESTED_MESSAGES, secondResponse.getRequestedMessages());
        Assertions.assertEquals(STATUS_ACCEPTED, secondResponse.getStatus());

        verify(manualDequeueOrchestratorExecutorMock, times(1))
                .execute(any(Runnable.class));

        verifyNoInteractions(manualDequeueOrchestratorMock);
    }

    @Test
    void requestDequeue_whenExecutorSubmissionFails_releasesOwnership() {
        doThrow(new RuntimeException("Executor submission error"))
                .when(manualDequeueOrchestratorExecutorMock)
                .execute(any(Runnable.class));

        Assertions.assertDoesNotThrow(
                () -> manualDequeueService.requestDequeue(
                        INITIATIVE_ID,
                        REQUESTED_MESSAGES
                )
        );

        /*
         * The first submission fails, therefore ownership must be released.
         * A second request for the same initiative must try to submit again.
         */
        Assertions.assertDoesNotThrow(
                () -> manualDequeueService.requestDequeue(
                        INITIATIVE_ID,
                        REQUESTED_MESSAGES
                )
        );

        verify(manualDequeueOrchestratorExecutorMock, times(2))
                .execute(any(Runnable.class));

        verifyNoInteractions(manualDequeueOrchestratorMock);
    }

    @Test
    void requestDequeue_whenRequestCompletionCallbackRuns_releasesOwnership() {
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();

            return null;
        }).when(manualDequeueOrchestratorExecutorMock).execute(any(Runnable.class));

        manualDequeueService.requestDequeue(
                INITIATIVE_ID,
                REQUESTED_MESSAGES
        );

        ArgumentCaptor<ManualDequeueRequest> requestCaptor =
                ArgumentCaptor.forClass(ManualDequeueRequest.class);

        verify(manualDequeueOrchestratorMock)
                .orchestrate(requestCaptor.capture());

        ManualDequeueRequest firstRequest = requestCaptor.getValue();

        /*
         * Simulates completion by NormalSessionWorker or ManualOnlySessionWorker.
         */
        firstRequest.onCompleted().run();

        manualDequeueService.requestDequeue(
                INITIATIVE_ID,
                REQUESTED_MESSAGES
        );

        verify(manualDequeueOrchestratorExecutorMock, times(2))
                .execute(any(Runnable.class));

        verify(manualDequeueOrchestratorMock, times(2))
                .orchestrate(any(ManualDequeueRequest.class));
    }
}
