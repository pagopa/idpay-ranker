package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.enums.PreallocationStatus;

public interface InitiativeCountersPreallocationsRepositoryExt {

    boolean findByIdAndStatusThenUpdateStatusToCaptured(String id, PreallocationStatus status);
}
