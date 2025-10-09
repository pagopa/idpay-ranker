package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface InitiativeCountersRepository extends MongoRepository<InitiativeCounters, String>, InitiativeCountersAtomicRepository {

    @Query(value = "{ '_id': ?0, 'preallocationMap.?1': { $exists: true } }", exists = true)
    boolean existsByInitiativeIdAndUserId(String initiativeId, String userId);

    @Override
    @Query(value = "{ '_id': ?0 }", fields = "{ 'preallocationMap': 0 }")
    Optional<InitiativeCounters> findById(String id);
}

