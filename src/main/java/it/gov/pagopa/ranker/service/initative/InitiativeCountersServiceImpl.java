package it.gov.pagopa.ranker.service.initative;

import com.mongodb.DuplicateKeyException;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.exception.BudgetExhaustedException;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class InitiativeCountersServiceImpl implements InitiativeCountersService {

    private final String initiativeId;

    private final InitiativeCountersRepository initiativeCounterRepository;

    public InitiativeCountersServiceImpl(InitiativeCountersRepository initiativeCounterRepository, @Value("${app.initiative.identified}") String initiativeId) {
        this.initiativeCounterRepository = initiativeCounterRepository;
        this.initiativeId = initiativeId;
    }

    @Override
    public void addPreallocatedUser(String initiativeId, String userId, boolean verifyIsee, Long sequenceNumber, LocalDateTime enqueuedTime) {
        long reservationCents = calculateReservationCents(verifyIsee);

        try {
            initiativeCounterRepository.incrementOnboardedAndBudget(
                    initiativeId,
                    userId,
                    reservationCents,
                    sequenceNumber,
                    enqueuedTime
            );
        } catch (DuplicateKeyException e){
            log.error("[RANKER] Budget exhausted for the initiative {}", initiativeId);
            throw new BudgetExhaustedException("[RANKER] Budget exhausted for the initiative: " + initiativeId, e);
        }
    }

    @Override
    public boolean hasAvailableBudget() {
        InitiativeCounters initiativeCounter = initiativeCounterRepository.findById(initiativeId).orElse(null);
        return initiativeCounter != null && initiativeCounter.getResidualInitiativeBudgetCents() >= 100;
    }

    public long calculateReservationCents(boolean verifyIsee) {
        return verifyIsee ? 200L : 100L;
    }
}
