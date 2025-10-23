package it.gov.pagopa.ranker.strategy.command;

import it.gov.pagopa.ranker.domain.dto.QueueCommandOperationDTO;

public interface CommandProcessorStrategy {

    String getCommandType();

    void processCommand(QueueCommandOperationDTO queueCommandOperationDTO);

}
