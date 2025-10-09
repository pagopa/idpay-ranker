package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.TransactionInProgress;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionInProgressRepository extends MongoRepository<TransactionInProgress, String> {}