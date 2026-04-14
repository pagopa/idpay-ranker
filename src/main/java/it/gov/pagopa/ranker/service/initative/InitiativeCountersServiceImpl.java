package it.gov.pagopa.ranker.service.initative;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.domain.model.InitiativeConfig;
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

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static it.gov.pagopa.utils.CommonUtils.sanitizeString;

@Slf4j
@Service
public class InitiativeCountersServiceImpl implements InitiativeCountersService {

    public static final String ID_SEPARATOR = "_";
    private final List<String> initiativeIds;

    private final InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;
    private final InitiativeCountersRepository initiativeCountersRepository;
    private final InitiativeBeneficiaryRuleService initiativeBeneficiaryRuleService;

    private final Clock clock;

    public InitiativeCountersServiceImpl(InitiativeCountersRepository initiativeCounterRepository,
                                         @Value("${app.initiative.identified}") List<String> initiativeIds,
                                         InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository,
                                         InitiativeBeneficiaryRuleService initiativeBeneficiaryRuleService, Clock clock) {
        this.initiativeCountersRepository = initiativeCounterRepository;
        this.initiativeIds = initiativeIds;
        this.initiativeCountersPreallocationsRepository = initiativeCountersPreallocationsRepository;
        this.initiativeBeneficiaryRuleService = initiativeBeneficiaryRuleService;
        this.clock = clock;
    }

    public boolean existsByInitiativeIdAndUserId(String initiativeId, String userId){
        return initiativeCountersPreallocationsRepository.existsById(userId+ID_SEPARATOR+initiativeId);
    }

    public Optional<InitiativeCountersPreallocations> findById(String initiativeId, String userId){
        return initiativeCountersPreallocationsRepository.findById(userId+ID_SEPARATOR+initiativeId);
    }

    @Transactional
    public void addPreallocatedUser(String initiativeId, String userId, boolean verifyIsee, Long sequenceNumber, Instant enqueuedTime) {
        long reservationCents = calculateReservationCents(verifyIsee, initiativeBeneficiaryRuleService.getInitiativeConfig(initiativeId));

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
                            .createdAt(Instant.now(clock))
                            .updateDate(Instant.now(clock))
                            .status(PreallocationStatus.PREALLOCATED)
                            .preallocatedAmountCents(reservationCents)
                            .build()
            );

        } catch (DuplicateKeyException e){
            //CosmosDB throw DuplicateKey even if the residualInitiativeBudgetCents is less than the minimum required and is not really a duplicated id
            log.error("[RANKER] Budget exhausted for the initiative {}", sanitizeString(initiativeId));
            throw new BudgetExhaustedException("[RANKER] Budget exhausted for the initiative: " + initiativeId, e);
        }
    }

    @Override
    public boolean hasAvailableBudget() {
        return getInitiativeCountersStream().findFirst().isPresent();
    }

    @Override
    public boolean hasAvailableBudget(String initiativeId) {
        InitiativeCounters counter = initiativeCountersRepository.findById(initiativeId).orElse(null);
        if(counter == null){
            return false;
        }
        return hasInitiativeBudgetToPreallocate(counter);
    }

    public long calculateReservationCents(boolean verifyIsee, InitiativeConfig initiativeConfig) {
        if(verifyIsee && initiativeConfig.getBeneficiaryInitiativeBudgetMaxCents() != null){
            return initiativeConfig.getBeneficiaryInitiativeBudgetMaxCents();
        } else {
            return initiativeConfig.getBeneficiaryInitiativeBudgetCents();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateInitiativeCounters(TransactionInProgressDTO transactionInProgress, String preallocationId, String transactionInProgressId) {
        if (!initiativeCountersPreallocationsRepository.existsById(preallocationId)) {
            log.warn("[ExpiredTransactionInProgressProcessor] received event for a transaction having initiative {}" +
                            " and user {} that does not exist in the initiative preallocation, will not update counter",
                    transactionInProgress.getInitiativeId(), transactionInProgress.getUserId());
        } else {
                initiativeCountersPreallocationsRepository.deleteById(preallocationId);
                initiativeCountersRepository.decrementOnboardedAndBudget(
                        transactionInProgress.getInitiativeId(),
                        transactionInProgress.getVoucherAmountCents());
        }
    }


    @Override
    public List<String> retrieveInitiativesAvailableBudget() {
        return getInitiativeCountersStream()
                .map(InitiativeCounters::getId)
                .toList();
    }

    private Stream<InitiativeCounters> getInitiativeCountersStream() {
        return initiativeCountersRepository.findByIdIn(initiativeIds).stream()
                .filter(this::hasInitiativeBudgetToPreallocate);
    }

    private boolean hasInitiativeBudgetToPreallocate(InitiativeCounters initiativeCounters) {
        InitiativeConfig initiativeConfig = initiativeBeneficiaryRuleService.getInitiativeConfig(initiativeCounters.getId());
        if (initiativeConfig != null) {
            if (initiativeConfig.getBeneficiaryInitiativeBudgetMaxCents() != null) {
                return initiativeCounters.getResidualInitiativeBudgetCents() >= initiativeConfig.getBeneficiaryInitiativeBudgetMaxCents();
            }
            return initiativeCounters.getResidualInitiativeBudgetCents() >= initiativeConfig.getBeneficiaryInitiativeBudgetCents();
        }
        return false;
    }
}
