package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.domain.model.InitiativeCountersPreallocations;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface InitiativeCountersPreallocationsRepository extends MongoRepository<InitiativeCountersPreallocations, String> {

}

