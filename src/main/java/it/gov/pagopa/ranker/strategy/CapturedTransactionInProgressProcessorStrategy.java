package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.repository.TransactionInProgressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.utils.InitiativeCountersUtils.computePreallocationId;

@Slf4j
@Service
public class CapturedTransactionInProgressProcessorStrategy implements TransactionInProgressProcessorStrategy {

    public static final String PREALLOCATED = "PREALLOCATED";
    private final InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;
    private final InitiativeCountersRepository initiativeCountersRepository;
    private final TransactionInProgressRepository transactionInProgressRepository;

    public CapturedTransactionInProgressProcessorStrategy(
            InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository, InitiativeCountersRepository initiativeCountersRepository,
            TransactionInProgressRepository transactionInProgressRepository) {
        this.initiativeCountersPreallocationsRepository = initiativeCountersPreallocationsRepository;
        this.initiativeCountersRepository = initiativeCountersRepository;
        this.transactionInProgressRepository = transactionInProgressRepository;
    }

    @Override
    public SyncTrxStatus getProcessedStatus() {
        return SyncTrxStatus.CAPTURED;
    }

    @Override
    public void processTransaction(TransactionInProgressDTO transactionInProgress) {

        String transactionInProgressId = transactionInProgress.getId();
        if (!transactionInProgressRepository.existsByIdAndStatus(
                transactionInProgressId, SyncTrxStatus.CAPTURED)) {
            log.warn("[CapturedTransactionInProgressProcessor] Provided transaction with id {} with status EXPIRED" +
                            " not found, no counter will be updated",
                    transactionInProgressId);
            return;
        }

        if (!initiativeCountersPreallocationsRepository.findByIdAndStatusThenUpdateStatusToCaptured(
                computePreallocationId(transactionInProgress), PREALLOCATED)) {
            log.warn("[CapturedTransactionInProgressProcessor] received event for a transaction having initiative {}" +
                    " and user {} that does not exist in the initiative preallocation or already processed, will not update counter",
                    transactionInProgress.getInitiativeId(), transactionInProgress.getUserId());
        } else {
            try {
                initiativeCountersRepository.updateCounterForCaptured(
                        transactionInProgress.getInitiativeId(),
                        transactionInProgress.getRewardCents(),
                        transactionInProgress.getVoucherAmountCents());
            } catch (Exception e) {
                log.error("[CapturedTransactionInProgressProcessor] Error attempting to " +
                          "alter initiativeCounters given id {} initiativeId {} and userId {}",
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
