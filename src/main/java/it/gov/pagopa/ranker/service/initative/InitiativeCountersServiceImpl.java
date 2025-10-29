package it.gov.pagopa.ranker.service.initative;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.domain.model.InitiativeCountersPreallocations;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import it.gov.pagopa.ranker.exception.BudgetExhaustedException;
import it.gov.pagopa.ranker.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class InitiativeCountersServiceImpl implements InitiativeCountersService {

    public static final String ID_SEPARATOR = "_";
    private final List<String> initiativeId;

    private final InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;
    private final InitiativeCountersRepository initiativeCountersRepository;

    public InitiativeCountersServiceImpl(InitiativeCountersRepository initiativeCounterRepository,
                                         @Value("${app.initiative.identified}") List<String> initiativeId, InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository) {
        this.initiativeCountersRepository = initiativeCounterRepository;
        this.initiativeId = initiativeId;
        this.initiativeCountersPreallocationsRepository = initiativeCountersPreallocationsRepository;
    }

    public boolean existsByInitiativeIdAndUserId(String initiativeId, String userId){
        return initiativeCountersPreallocationsRepository.existsById(userId+ID_SEPARATOR+initiativeId);
    }

    @Transactional
    public void addPreallocatedUser(String initiativeId, String userId, boolean verifyIsee, Long sequenceNumber, LocalDateTime enqueuedTime) {
        long reservationCents = calculateReservationCents(verifyIsee);

        try {
            initiativeCountersRepository.incrementOnboardedAndBudget(
                    initiativeId,
                    reservationCents
            );

            initiativeCountersPreallocationsRepository.insert(
                    InitiativeCountersPreallocations.builder()
                            .id(userId+ID_SEPARATOR+initiativeId)
                            .initiativeId(initiativeId)
                            .userId(userId)
                            .sequenceNumber(sequenceNumber)
                            .enqueuedTime(enqueuedTime)
                            .createdAt(LocalDateTime.now())
                            .status(PreallocationStatus.PREALLOCATED)
                            .preallocatedAmountCents(reservationCents)
                            .build()
            );

        } catch (DuplicateKeyException e){
            //CosmosDB throw DuplicateKey even if the residualInitiativeBudgetCents is less than the minimum required and is not really a duplicated id
            log.error("[RANKER] Budget exhausted for the initiative {}", initiativeId);
            throw new BudgetExhaustedException("[RANKER] Budget exhausted for the initiative: " + initiativeId, e);
        }
    }

    @Override
    public boolean hasAvailableBudget() {
        return initiativeCountersRepository.existsByIdInAndResidualInitiativeBudgetCentsGreaterThanEqual(
                initiativeId, 20000);
    }

    public long calculateReservationCents(boolean verifyIsee) {
        return verifyIsee ? 20000L : 10000L;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateInitiativeCounters(TransactionInProgressDTO transactionInProgress, String preallocationId, String transactionInProgressId) {
        if (!initiativeCountersPreallocationsRepository.existsById(preallocationId)) {
            log.warn("[ExpiredTransactionInProgressProcessor] received event for a transaction having initiative {}" +
                            " and user {} that does not exist in the initiative preallocation, will not update counter",
                    transactionInProgress.getInitiativeId(), transactionInProgress.getUserId());
        } else {
            try {
                initiativeCountersPreallocationsRepository.deleteById(preallocationId);
                initiativeCountersRepository.decrementOnboardedAndBudget(
                        transactionInProgress.getInitiativeId(),
                        transactionInProgress.getVoucherAmountCents());
            } catch (Exception e) {
                log.error("[ExpiredTransactionInProgressProcessor] Error attempting to " +
                                "decrement initiativeCounters given id {} initiativeId {} and userId {}",
                        transactionInProgressId,
                        transactionInProgress.getInitiativeId(),
                        transactionInProgress.getUserId(),
                        e
                );
                throw e;
            }
        }
    }
}
