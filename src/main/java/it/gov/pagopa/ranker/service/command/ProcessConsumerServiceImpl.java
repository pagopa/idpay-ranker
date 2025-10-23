package it.gov.pagopa.ranker.service.command;

import it.gov.pagopa.ranker.domain.dto.QueueCommandOperationDTO;
import it.gov.pagopa.ranker.strategy.command.CommandProcessorStrategy;
import it.gov.pagopa.ranker.strategy.command.CommandProcessorStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProcessConsumerServiceImpl implements ProcessConsumerService {

    private final CommandProcessorStrategyFactory commandProcessorStrategyFactory;

    public ProcessConsumerServiceImpl(CommandProcessorStrategyFactory commandProcessorStrategyFactory) {
        this.commandProcessorStrategyFactory = commandProcessorStrategyFactory;
    }

    @Override
    public void processCommand(QueueCommandOperationDTO queueCommandOperationDTO) {
        CommandProcessorStrategy commandProcessorStrategy =
                commandProcessorStrategyFactory.getStrategy(queueCommandOperationDTO.getOperationType());
        commandProcessorStrategy.processCommand(queueCommandOperationDTO);
    }

}
