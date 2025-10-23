package it.gov.pagopa.ranker.service.command;

import it.gov.pagopa.ranker.domain.dto.QueueCommandOperationDTO;

public interface ProcessConsumerService {
    void processCommand(QueueCommandOperationDTO queueCommandOperationDTO);
}
