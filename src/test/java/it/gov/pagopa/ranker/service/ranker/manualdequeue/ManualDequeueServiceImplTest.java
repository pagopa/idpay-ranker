package it.gov.pagopa.ranker.service.ranker.manualdequeue;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import it.gov.pagopa.ranker.domain.dto.ManualDequeueResponseDTO;
import it.gov.pagopa.ranker.exception.ManualDequeueSizeNotValidException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.Executor;

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
    private static final String TRACE_ID = "0123456789abcdef0123456789abcdef";
    private static final String SPAN_ID = "0123456789abcdef";
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
        SpanContext spanContext = SpanContext.create(
                TRACE_ID,
                SPAN_ID,
                TraceFlags.getSampled(),
                TraceState.getDefault()
        );

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();

            return null;
        }).when(manualDequeueOrchestratorExecutorMock)
                .execute(any(Runnable.class));

        ManualDequeueResponseDTO response;

        Scope scope = Span.wrap(spanContext).makeCurrent();

        try {
            response = manualDequeueService.requestDequeue(
                    INITIATIVE_ID,
                    REQUESTED_MESSAGES
            );
        } finally {
            scope.close();
        }

        Assertions.assertEquals(INITIATIVE_ID, response.getInitiativeId());
        Assertions.assertEquals(
                REQUESTED_MESSAGES,
                response.getRequestedMessages()
        );
        Assertions.assertEquals(STATUS_ACCEPTED, response.getStatus());
        Assertions.assertEquals(TRACE_ID, response.getTraceId());

        ArgumentCaptor<ManualDequeueRequest> requestCaptor =
                ArgumentCaptor.forClass(ManualDequeueRequest.class);

        verify(manualDequeueOrchestratorMock)
                .orchestrate(requestCaptor.capture());

        ManualDequeueRequest capturedRequest = requestCaptor.getValue();

        Assertions.assertEquals(
                INITIATIVE_ID,
                capturedRequest.initiativeId()
        );
        Assertions.assertEquals(
                REQUESTED_MESSAGES,
                capturedRequest.size()
        );
        Assertions.assertEquals(
                TRACE_ID,
                capturedRequest.traceId()
        );
    }

    @Test
    void requestDequeue_withTwoRequestsForSameInitiative_submitsBothRequests() {
        manualDequeueService.requestDequeue(
                INITIATIVE_ID,
                REQUESTED_MESSAGES
        );

        ManualDequeueResponseDTO secondResponse =
                manualDequeueService.requestDequeue(
                        INITIATIVE_ID,
                        REQUESTED_MESSAGES
                );

        Assertions.assertEquals(
                INITIATIVE_ID,
                secondResponse.getInitiativeId()
        );
        Assertions.assertEquals(
                REQUESTED_MESSAGES,
                secondResponse.getRequestedMessages()
        );
        Assertions.assertEquals(
                STATUS_ACCEPTED,
                secondResponse.getStatus()
        );

        verify(manualDequeueOrchestratorExecutorMock, times(2))
                .execute(any(Runnable.class));

        verifyNoInteractions(manualDequeueOrchestratorMock);
    }

    @Test
    void requestDequeue_whenExecutorSubmissionFails_doesNotPropagateException() {
        doThrow(new RuntimeException("Executor submission error"))
                .when(manualDequeueOrchestratorExecutorMock)
                .execute(any(Runnable.class));

        Assertions.assertDoesNotThrow(
                () -> manualDequeueService.requestDequeue(
                        INITIATIVE_ID,
                        REQUESTED_MESSAGES
                )
        );

        verify(manualDequeueOrchestratorExecutorMock)
                .execute(any(Runnable.class));

        verifyNoInteractions(manualDequeueOrchestratorMock);
    }

    @Test
    void requestDequeue_afterExecutorSubmissionFailure_submitsLaterRequestAgain() {
        doThrow(new RuntimeException("Executor submission error"))
                .when(manualDequeueOrchestratorExecutorMock)
                .execute(any(Runnable.class));

        Assertions.assertDoesNotThrow(
                () -> manualDequeueService.requestDequeue(
                        INITIATIVE_ID,
                        REQUESTED_MESSAGES
                )
        );

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
    void requestDequeue_withTwoRequestsOrchestratesBothRequests() {
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();

            return null;
        }).when(manualDequeueOrchestratorExecutorMock)
                .execute(any(Runnable.class));

        manualDequeueService.requestDequeue(
                INITIATIVE_ID,
                REQUESTED_MESSAGES
        );

        manualDequeueService.requestDequeue(
                INITIATIVE_ID,
                REQUESTED_MESSAGES
        );

        ArgumentCaptor<ManualDequeueRequest> requestCaptor =
                ArgumentCaptor.forClass(ManualDequeueRequest.class);

        verify(manualDequeueOrchestratorMock, times(2))
                .orchestrate(requestCaptor.capture());

        List<ManualDequeueRequest> requests =
                requestCaptor.getAllValues();

        Assertions.assertEquals(2, requests.size());

        Assertions.assertEquals(
                INITIATIVE_ID,
                requests.get(0).initiativeId()
        );

        Assertions.assertEquals(
                INITIATIVE_ID,
                requests.get(1).initiativeId()
        );
    }
}
