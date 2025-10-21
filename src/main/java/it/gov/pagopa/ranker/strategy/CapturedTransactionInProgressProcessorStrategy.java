package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.repository.TransactionInProgressRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersServiceImpl;
import it.gov.pagopa.utils.InitiativeCountersUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.utils.InitiativeCountersUtils.computePreallocationId;

@Slf4j
@Service
public class CapturedTransactionInProgressProcessorStrategy implements TransactionInProgressProcessorStrategy {

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

        if (!initiativeCountersPreallocationsRepository.existsById(
                computePreallocationId(transactionInProgress))) {
            log.warn("[CapturedTransactionInProgressProcessor] received event for a transaction having initiative {}" +
                    " and user {} that does not exist in the initiative preallocation, will not update counter",
                    transactionInProgress.getInitiativeId(), transactionInProgress.getUserId());
        } else {
            try {
                initiativeCountersRepository.updateCounterForCaptured(
                        transactionInProgress.getInitiativeId(),
                        transactionInProgress.getEffectiveAmountCents(),
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
