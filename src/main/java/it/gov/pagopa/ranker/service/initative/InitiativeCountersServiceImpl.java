package it.gov.pagopa.ranker.service.initative;

import it.gov.pagopa.ranker.domain.model.InitiativeCountersPreallocations;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import it.gov.pagopa.ranker.exception.BudgetExhaustedException;
import it.gov.pagopa.ranker.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class InitiativeCountersServiceImpl implements InitiativeCountersService {

    public static final String ID_SEPARATOR = "_";
    private final List<String> initiativeId;

    private final InitiativeCountersRepository initiativeCountersRepository;

    private final InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;

    public InitiativeCountersServiceImpl(InitiativeCountersRepository initiativeCounterRepository,
                                         InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository,
                                         @Value("${app.initiative.identified}") List<String> initiativeId) {
        this.initiativeCountersRepository = initiativeCounterRepository;
        this.initiativeCountersPreallocationsRepository = initiativeCountersPreallocationsRepository;
        this.initiativeId = initiativeId;
    }

    public boolean existsByInitiativeIdAndUserId(String initiativeId, String userId){
        return initiativeCountersPreallocationsRepository.existsById(userId+ID_SEPARATOR+initiativeId);
    }

    public void addPreallocatedUser(String initiativeId, String userId, boolean verifyIsee, Long sequenceNumber, LocalDateTime enqueuedTime) {
        long reservationCents = calculateReservationCents(verifyIsee);

        try {
            initiativeCountersRepository.incrementOnboardedAndBudget(
                    initiativeId,
                    reservationCents
            );

            initiativeCountersPreallocationsRepository.save(InitiativeCountersPreallocations.builder()
                    .id(userId+ID_SEPARATOR+initiativeId)
                    .initiativeId(initiativeId)
                    .userId(userId)
                    .sequenceNumber(sequenceNumber)
                    .enqueuedTime(enqueuedTime)
                    .createdAt(LocalDateTime.now())
                    .status(PreallocationStatus.PREALLOCATED)
                    .build()
            );
        } catch (DuplicateKeyException e){
            log.error("[RANKER] Budget exhausted for the initiative {}", initiativeId);
            throw new BudgetExhaustedException("[RANKER] Budget exhausted for the initiative: " + initiativeId, e);
        }
    }

    @Override
    public boolean hasAvailableBudget() {
        return initiativeCountersRepository.existsByIdInAndResidualInitiativeBudgetCentsGreaterThanEqual(initiativeId, 10000);
    }

    public long calculateReservationCents(boolean verifyIsee) {
        return verifyIsee ? 20000L : 10000L;
    }
}
