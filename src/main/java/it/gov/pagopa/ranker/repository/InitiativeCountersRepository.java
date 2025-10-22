package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface InitiativeCountersRepository extends MongoRepository<InitiativeCounters, String>, InitiativeCountersAtomicRepository {

    @Query(value = "{ '_id': { $in: ?0 }, 'residualInitiativeBudgetCents': { $gte: ?1 } }", exists = true)
    boolean existsByIdInAndResidualInitiativeBudgetCentsGreaterThanEqual(List<String> ids, long minResidual);

    @Query(value = "{ '_id': { $in: ?0 }, 'sequenceIdToProcess': { $ne : null } }")
    List<InitiativeCounters> findExistingSequenceIdToProcess(List<String> ids);

}

