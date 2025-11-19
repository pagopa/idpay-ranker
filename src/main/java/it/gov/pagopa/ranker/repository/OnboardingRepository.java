package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.Onboarding;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OnboardingRepository extends MongoRepository<Onboarding, String> {
}
