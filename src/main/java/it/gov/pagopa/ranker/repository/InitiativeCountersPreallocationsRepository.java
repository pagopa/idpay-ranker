package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCountersPreallocations;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InitiativeCountersPreallocationsRepository
        extends MongoRepository<InitiativeCountersPreallocations, String> , InitiativeCountersPreallocationsRepositoryExt{

}

