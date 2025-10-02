package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface InitiativeCountersRepository extends MongoRepository<InitiativeCounters, String> {

    @Query("{ '_id': ?0, 'preallocationMap.?1': { $exists: true } }")
    InitiativeCounters findByInitiativeIdAndUserId(String initiativeId, String userId);

    @Query(value = "{ '_id': ?0, 'preallocationMap.?1': { $exists: true } }", exists = true)
    boolean existsByInitiativeIdAndUserId(String initiativeId, String userId);

    @Query("{ 'residualInitiativeBudgetCents': { $gte: ?0 } }")
    List<InitiativeCounters> findByResidualBudgetGreaterThanEqual(long threshold);
}

