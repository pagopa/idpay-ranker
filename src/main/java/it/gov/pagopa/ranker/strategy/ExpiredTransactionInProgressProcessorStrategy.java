package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.connector.rest.PaymentRestClient;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static it.gov.pagopa.utils.InitiativeCountersUtils.computePreallocationId;

@Slf4j
@Service
public class ExpiredTransactionInProgressProcessorStrategy implements TransactionInProgressProcessorStrategy {


    private final PaymentRestClient paymentRestClient;
    private final InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;
    private final InitiativeCountersRepository initiativeCountersRepository;

    public ExpiredTransactionInProgressProcessorStrategy(
            PaymentRestClient paymentRestClient,
            InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository,
            InitiativeCountersRepository initiativeCountersRepository) {
        this.paymentRestClient = paymentRestClient;
        this.initiativeCountersPreallocationsRepository = initiativeCountersPreallocationsRepository;
        this.initiativeCountersRepository = initiativeCountersRepository;
    }

    @Override
    public SyncTrxStatus getProcessedStatus() {
        return SyncTrxStatus.EXPIRED;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processTransaction(TransactionInProgressDTO transactionInProgress) {

        String transactionInProgressId = transactionInProgress.getId();
        String preallocationId = computePreallocationId(transactionInProgress);
        if (!paymentRestClient.existsByIdAndStatus(
                transactionInProgressId, SyncTrxStatus.EXPIRED)) {
            log.warn("[ExpiredTransactionInProgressProcessor] Provided transaction with id {} with status EXPIRED not found",
                    transactionInProgressId);
            return;
        }

        if (!initiativeCountersPreallocationsRepository.findByIdAndStatusThenUpdateStatus(
                preallocationId,
                PreallocationStatus.PREALLOCATED,
                PreallocationStatus.EXPIRED)) {
            log.warn("[ExpiredTransactionInProgressProcessor] received event for a transaction having initiative {}" +
                            " and user {} that does not exist in the initiative preallocation or already processed, will not update counter",
                    transactionInProgress.getInitiativeId(), transactionInProgress.getUserId());
            return;
        }

        try {
            initiativeCountersRepository.decrementOnboardedAndBudget(
                    transactionInProgress.getInitiativeId(),
                    transactionInProgress.getVoucherAmountCents());

            log.info("[ExpiredTransactionInProgressProcessor] Reverted counters for expired transaction {}", transactionInProgressId);

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
