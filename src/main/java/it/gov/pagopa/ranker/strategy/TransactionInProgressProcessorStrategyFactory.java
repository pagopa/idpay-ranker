package it.gov.pagopa.ranker.strategy;

import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.exception.UnmanagedStrategyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TransactionInProgressProcessorStrategyFactory {
    private final Map<SyncTrxStatus,TransactionInProgressProcessorStrategy> strategies;

    @Autowired
    public TransactionInProgressProcessorStrategyFactory(
            List<TransactionInProgressProcessorStrategy> strategies) {
        this.strategies = strategies.stream().collect(Collectors.toMap(
                TransactionInProgressProcessorStrategy::getProcessedStatus, Function.identity()));
    }

    public TransactionInProgressProcessorStrategy getStrategy(SyncTrxStatus trxStatus) {
        assert trxStatus != null;
        TransactionInProgressProcessorStrategy transactionInProgressProcessorStrategy =
                strategies.get(trxStatus);
        if (transactionInProgressProcessorStrategy == null) {
            throw new UnmanagedStrategyException("Status" + trxStatus + "not managed as a strategy");
        }
        return transactionInProgressProcessorStrategy;
    }

}