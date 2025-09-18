package it.gov.pagopa.ranker.repo;

import it.gov.pagopa.ranker.domain.model.Entity;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface MongoRepository extends ReactiveMongoRepository<Entity, String> {

    @Query("{ 'realFieldName': ?0 }")
    Mono<Entity> findBySomeField(String someField);
}
