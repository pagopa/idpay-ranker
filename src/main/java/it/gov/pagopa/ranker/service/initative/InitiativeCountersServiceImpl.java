package it.gov.pagopa.ranker.service.initative;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.domain.dto.VerifyDTO;
import it.gov.pagopa.ranker.domain.model.InitiativeConfig;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.domain.model.InitiativeCountersPreallocations;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import it.gov.pagopa.ranker.exception.BudgetExhaustedException;
import it.gov.pagopa.ranker.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
public class InitiativeCountersServiceImpl implements InitiativeCountersService {

    public static final String ID_SEPARATOR = "_";
    private final List<String> initiativeIds;

    private final InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;
    private final InitiativeCountersRepository initiativeCountersRepository;
    private final InitiativeBeneficiaryRuleService initiativeBeneficiaryRuleService;

    public InitiativeCountersServiceImpl(InitiativeCountersRepository initiativeCounterRepository,
                                         @Value("${app.initiative.identified}") List<String> initiativeIds,
                                         InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository,
                                         InitiativeBeneficiaryRuleService initiativeBeneficiaryRuleService) {
        this.initiativeCountersRepository = initiativeCounterRepository;
        this.initiativeIds = initiativeIds;
        this.initiativeCountersPreallocationsRepository = initiativeCountersPreallocationsRepository;
        this.initiativeBeneficiaryRuleService = initiativeBeneficiaryRuleService;
    }

    public boolean existsByInitiativeIdAndUserId(String initiativeId, String userId){
        return initiativeCountersPreallocationsRepository.existsById(userId+ID_SEPARATOR+initiativeId);
    }

    public Optional<InitiativeCountersPreallocations> findById(String initiativeId, String userId){
        return initiativeCountersPreallocationsRepository.findById(userId+ID_SEPARATOR+initiativeId);
    }

    @Transactional
    public void addPreallocatedUser(String initiativeId, String userId, List<VerifyDTO> verifies, Long sequenceNumber, LocalDateTime enqueuedTime) {


        long reservationCents = calculateReservationCents(verifies, initiativeBeneficiaryRuleService.getInitiativeConfig(initiativeId));

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
                            .updateDate(LocalDateTime.now())
                            .status(PreallocationStatus.PREALLOCATED)
                            .preallocatedAmountCents(reservationCents)
                            .build()
            );

        } catch (DuplicateKeyException e){
            //CosmosDB throw DuplicateKey even if the residualInitiativeBudgetCents is less than the minimum required and is not really a duplicated id
            log.error("[RANKER] Budget exhausted for the initiative {}", CommonUtils.sanitizeString(initiativeId));
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

    public long calculateReservationCents(List<VerifyDTO> verifies, InitiativeConfig initiativeConfig) {

        return verifies.stream()
                .map(VerifyDTO::getBeneficiaryBudgetCentsMax)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(initiativeConfig.getBeneficiaryBudgetFixedCents());
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
            if (initiativeConfig.getBeneficiaryBudgetMaxCents() != null) {
                return initiativeCounters.getResidualInitiativeBudgetCents() >= initiativeConfig.getBeneficiaryBudgetMaxCents();
            } else if (initiativeConfig.getBeneficiaryBudgetFixedCents() != null) {
                return initiativeCounters.getResidualInitiativeBudgetCents() >= initiativeConfig.getBeneficiaryBudgetFixedCents();
            }
        }
        return false;
    }
}
