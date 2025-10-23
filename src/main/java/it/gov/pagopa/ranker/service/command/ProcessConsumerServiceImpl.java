package it.gov.pagopa.ranker.service.command;

import it.gov.pagopa.ranker.domain.dto.QueueCommandOperationDTO;
import it.gov.pagopa.ranker.exception.UnmanagedStrategyException;
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
        try {
            CommandProcessorStrategy commandProcessorStrategy =
                    commandProcessorStrategyFactory.getStrategy(queueCommandOperationDTO.getOperationType());
            if (commandProcessorStrategy == null) {
                throw new UnmanagedStrategyException("Unamanaged strategy with operationType "
                        + queueCommandOperationDTO.getOperationType());
            }
            commandProcessorStrategy.processCommand(queueCommandOperationDTO);
        } catch (UnmanagedStrategyException e) {
            log.debug("[PROCESS_COMMAND] Command with operation type {} not managed",
                    queueCommandOperationDTO.getOperationType());
        } catch (Exception e) {
            log.error("[PROCESS_COMMAND] encountered unmanaged error during command processing");
            throw e;
        }
    }

}
