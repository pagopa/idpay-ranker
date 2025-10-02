package it.gov.pagopa.ranker.service.initative;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.repository.InitiativeCountersAtomicRepository;
import org.springframework.stereotype.Service;

@Service
public class InitiativeCountersServiceImpl implements InitiativeCountersService {

    private final InitiativeCountersAtomicRepository atomicRepository;

    public InitiativeCountersServiceImpl(InitiativeCountersAtomicRepository atomicRepository) {
        this.atomicRepository = atomicRepository;
    }

    @Override
    public long addedPreallocatedUser(String initiativeId, String userId, boolean verifyIsee, Long sequenceNumber, Long enqueuedTime) {
        long reservationCents = calculateReservationCents(verifyIsee);

        sequenceNumber = sequenceNumber != null ? sequenceNumber : 0L;
        enqueuedTime = enqueuedTime != null ? enqueuedTime : 0L;

        InitiativeCounters updated = atomicRepository.incrementOnboardedAndBudget(
                initiativeId,
                userId,
                reservationCents,
                sequenceNumber,
                enqueuedTime
        );

        if (updated == null) {
            throw new IllegalArgumentException("Initiative not found or insufficient budget: " + initiativeId);
        }

        return updated.getOnboarded();
    }

    public long calculateReservationCents(boolean verifyIsee) {
        return verifyIsee ? 200L : 100L;
    }
}
