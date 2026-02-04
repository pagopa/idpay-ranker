package it.gov.pagopa.ranker.repository;

public interface InitiativeCountersPreallocationsRepositoryExt {

    boolean findByIdAndStatusThenUpdateStatusToCaptured(String id, String status);
}
