package it.gov.pagopa.ranker.strategy.command;

import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.exception.UnmanagedStrategyException;
import it.gov.pagopa.ranker.strategy.TransactionInProgressProcessorStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CommandProcessorStrategyFactory {
    private final Map<String, CommandProcessorStrategy> strategies;

    @Autowired
    public CommandProcessorStrategyFactory(
            List<CommandProcessorStrategy> strategies) {
        this.strategies = strategies.stream().collect(Collectors.toMap(
                CommandProcessorStrategy::getCommandType, Function.identity()));
    }

    public CommandProcessorStrategy getStrategy(String commandType) {
        assert commandType != null;
        CommandProcessorStrategy commandProcessorStrategy =
                strategies.get(commandType);
        if (commandProcessorStrategy == null) {
            throw new UnmanagedStrategyException("Command type" + commandType + "not managed as a strategy");
        }
        return commandProcessorStrategy;
    }

}