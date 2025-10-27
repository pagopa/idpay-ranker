package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.repository.TransactionInProgressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static it.gov.pagopa.utils.InitiativeCountersUtils.computePreallocationId;

@Slf4j
@Service
public class ExpiredTransactionInProgressProcessorStrategy implements TransactionInProgressProcessorStrategy {

    private final InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;
    private final InitiativeCountersRepository initiativeCountersRepository;
    private final TransactionInProgressRepository transactionInProgressRepository;

    public ExpiredTransactionInProgressProcessorStrategy(
            InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository, InitiativeCountersRepository initiativeCountersRepository, TransactionInProgressRepository transactionInProgressRepository) {
        this.initiativeCountersPreallocationsRepository = initiativeCountersPreallocationsRepository;
        this.initiativeCountersRepository = initiativeCountersRepository;
        this.transactionInProgressRepository = transactionInProgressRepository;
    }

    @Override
    public SyncTrxStatus getProcessedStatus() {
        return SyncTrxStatus.EXPIRED;
    }

    @Override
    public void processTransaction(TransactionInProgressDTO transactionInProgress) {

        String transactionInProgressId = transactionInProgress.getId();
        String preallocationId = computePreallocationId(transactionInProgress);
        if (!transactionInProgressRepository.existsByIdAndStatus(
                transactionInProgressId, SyncTrxStatus.EXPIRED)) {
            log.warn("[ExpiredTransactionInProgressProcessor] Provided transaction with id {} with status EXPIRED" +
                            " not found, no counter will be updated",
                    transactionInProgressId);
            return;
        }

        updateInitiativeCounters(transactionInProgress, preallocationId, transactionInProgressId);

        try {
            transactionInProgressRepository.deleteById(transactionInProgressId);
        } catch (Exception e) {
            log.error("[ExpiredTransactionInProgressProcessor] Error attempting to " +
                      "remove processed expired transactions given id {} initiativeId {} and userId {}",
                    transactionInProgressId,
                    transactionInProgress.getInitiativeId(),
                    transactionInProgress.getUserId(),
                    e
            );
            throw e;
        }

    }

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
