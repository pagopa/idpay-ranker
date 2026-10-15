package it.gov.pagopa.ranker.service.ranker.manualdequeue;

public record ManualDequeueRequest(
        String initiativeId,
        int size,
        String traceId
) {
}

