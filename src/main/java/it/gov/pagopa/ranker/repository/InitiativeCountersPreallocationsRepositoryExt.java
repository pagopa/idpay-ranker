package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.enums.PreallocationStatus;

public interface InitiativeCountersPreallocationsRepositoryExt {

    boolean findByIdAndStatusThenUpdateStatus(String id, PreallocationStatus currentStatus, PreallocationStatus newStatus);

    default boolean findByIdAndStatusThenUpdateStatusToCaptured(String id, PreallocationStatus status) {
        return findByIdAndStatusThenUpdateStatus(id, status, PreallocationStatus.CAPTURED);
    }
}
