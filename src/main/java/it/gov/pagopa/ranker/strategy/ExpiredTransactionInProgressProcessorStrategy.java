package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.domain.model.TransactionInProgress;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.repository.TransactionInProgressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExpiredTransactionInProgressProcessorStrategy implements TransactionInProgressProcessorStrategy {

    private final InitiativeCountersRepository initiativeCountersRepository;
    private final TransactionInProgressRepository transactionInProgressRepository;

    public ExpiredTransactionInProgressProcessorStrategy(
            InitiativeCountersRepository initiativeCountersRepository, TransactionInProgressRepository transactionInProgressRepository) {
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
        if (!transactionInProgressRepository.existsByIdAndStatus(
                transactionInProgressId, SyncTrxStatus.EXPIRED)) {
            log.warn("[ExpiredTransactionInProgressProcessor] Provided transaction with id {} with status EXPIRED" +
                            " not found, no counter will be updated",
                    transactionInProgressId);
            return;
        }

        if (!initiativeCountersRepository.existsByInitiativeIdAndUserId(
                transactionInProgress.getInitiativeId(), transactionInProgress.getUserId())) {
            log.warn("[ExpiredTransactionInProgressProcessor] received event for a transaction having initiative {}" +
                    " and user {} that does not exist in the initiative map, will not update counter",
                    transactionInProgress.getInitiativeId(), transactionInProgress.getUserId());
        } else {
            try {
                initiativeCountersRepository.decrementOnboardedAndBudget(
                        transactionInProgress.getInitiativeId(),
                        transactionInProgress.getUserId(),
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
}
