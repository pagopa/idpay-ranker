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

        if (!initiativeCountersRepository.existsByInitiativeIdAndUserId(
                transactionInProgress.getInitiativeId(), transactionInProgress.getUserId())) {
            log.warn("[ExpiredTransactionInProgressProcessor] received event for a transaction having initiative {}" +
                    " and user {} that does not exist in the initiative map",
                    transactionInProgress.getInitiativeId(), transactionInProgress.getUserId());
            return;
        }

        try {
            initiativeCountersRepository.decrementOnboardedAndBudget(
                    transactionInProgress.getInitiativeId(),
                    transactionInProgress.getUserId(),
                    transactionInProgress.getVoucherAmountCents());
        } catch (Exception e) {
            log.error("[ExpiredTransactionInProgressProcessor] Error attempting to " +
                    "decrement initiativeCounters given id {} initiativeId {} and userId {}",
                        transactionInProgress.getId(),
                        transactionInProgress.getInitiativeId(),
                        transactionInProgress.getUserId(),
                    e
            );
            throw e;
        }

        try {
            transactionInProgressRepository.deleteById(transactionInProgress.getId());
        } catch (Exception e) {
            log.error("[ExpiredTransactionInProgressProcessor] Error attempting to " +
                            "remove processed expired transactions given id {} initiativeId {} and userId {}",
                    transactionInProgress.getId(),
                    transactionInProgress.getInitiativeId(),
                    transactionInProgress.getUserId(),
                    e
            );
            throw e;
        }

    }
}
