package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface InitiativeCountersRepository extends MongoRepository<InitiativeCounters, String> {

    @Query("{ 'initiativeId': ?0, 'userId': ?1 }")
    InitiativeCounters findByInitiativeIdAndUserId(String initiativeId, String userId);

}
