package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.enums.PreallocationStatus;

import java.time.LocalDateTime;

public interface InitiativeCountersPreallocationsRepositoryExt {

    boolean findByIdAndStatusThenUpdateStatus(String id, PreallocationStatus currentStatus, PreallocationStatus newStatus);

    boolean findByIdAndStatusAndEventAfterEnqueuedThenUpdateStatus(
            String id,
            PreallocationStatus currentStatus,
            LocalDateTime eventDate,
            PreallocationStatus newStatus
    );

    default boolean findByIdAndStatusThenUpdateStatusToCaptured(String id, PreallocationStatus status) {
        return findByIdAndStatusThenUpdateStatus(id, status, PreallocationStatus.CAPTURED);
    }
}
