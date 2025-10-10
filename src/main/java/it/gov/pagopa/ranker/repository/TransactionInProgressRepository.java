package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.TransactionInProgress;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionInProgressRepository extends MongoRepository<TransactionInProgress, String> {
    boolean findByIdAndStatus(@NotNull String id, @NotNull SyncTrxStatus status);
}