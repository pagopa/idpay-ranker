package it.gov.pagopa.ranker.service.initative;

public interface InitiativeCountersService {
    long addedPreallocatedUser(String initiativeId, String userId, boolean verifyIsee);
}
