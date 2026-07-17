package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.DroolsRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DroolsRuleRepository extends MongoRepository<DroolsRule, String> {
    List<DroolsRule> findByIdIn(List<String> initiativeIds);
}
