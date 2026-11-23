package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.utils.InitiativeCountersUtils.computePreallocationId;

@Slf4j
@Service
public class CapturedTransactionInProgressProcessorStrategy implements TransactionInProgressProcessorStrategy {
    private final InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;
    private final InitiativeCountersRepository initiativeCountersRepository;

    public CapturedTransactionInProgressProcessorStrategy(
            InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository, 
            InitiativeCountersRepository initiativeCountersRepository) {
        this.initiativeCountersPreallocationsRepository = initiativeCountersPreallocationsRepository;
        this.initiativeCountersRepository = initiativeCountersRepository;
    }

    @Override
    public SyncTrxStatus getProcessedStatus() {
        return SyncTrxStatus.CAPTURED;
    }

    @Override
    public void processTransaction(TransactionInProgressDTO transactionInProgress) {

        if (!initiativeCountersPreallocationsRepository.findByIdAndStatusThenUpdateStatus(
                computePreallocationId(transactionInProgress),
                PreallocationStatus.PREALLOCATED,
                PreallocationStatus.CAPTURED)) {
            log.warn("[CapturedTransactionInProgressProcessor] received event for a transaction having initiative {}" +
                    " and user {} that does not exist in the initiative preallocation or already processed, will not update counter",
                    transactionInProgress.getInitiativeId(), transactionInProgress.getUserId());
        } else {
            try {
                initiativeCountersRepository.updateCounterForCaptured(
                        transactionInProgress.getInitiativeId(),
                        transactionInProgress.getRewardCents(),
                        transactionInProgress.getVoucherAmountCents());

                log.info("[CapturedTransactionInProgressProcessor] Updated counter for captured transaction {}", 
                        transactionInProgress.getId());

            } catch (Exception e) {
                log.error("[CapturedTransactionInProgressProcessor] Error attempting to " +
                          "alter initiativeCounters given id {} initiativeId {} and userId {}",
                        transactionInProgress.getId(),
                        transactionInProgress.getInitiativeId(),
                        transactionInProgress.getUserId(),
                        e
                );
                throw e;
            }
        }
    }

}
