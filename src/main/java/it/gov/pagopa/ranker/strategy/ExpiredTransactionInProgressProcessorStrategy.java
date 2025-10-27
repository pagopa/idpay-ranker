package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.repository.TransactionInProgressRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static it.gov.pagopa.utils.InitiativeCountersUtils.computePreallocationId;

@Slf4j
@Service
public class ExpiredTransactionInProgressProcessorStrategy implements TransactionInProgressProcessorStrategy {


    private final TransactionInProgressRepository transactionInProgressRepository;
    private final InitiativeCountersService initiativeCountersService;

    public ExpiredTransactionInProgressProcessorStrategy(
            TransactionInProgressRepository transactionInProgressRepository, InitiativeCountersService initiativeCountersService) {
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.initiativeCountersService = initiativeCountersService;
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

        initiativeCountersService.updateInitiativeCounters(transactionInProgress, preallocationId, transactionInProgressId);

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
