package it.gov.pagopa.ranker.strategy.command;

import it.gov.pagopa.ranker.domain.dto.QueueCommandOperationDTO;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

import static it.gov.pagopa.ranker.constants.CommonConstants.SEQUENCE_NUMBER_PROPERTY;

@Service
@Slf4j
public class DeleteSequenceNumberCommandStrategy implements CommandProcessorStrategy {

    private final InitiativeCountersService initiativeCountersService;

    public DeleteSequenceNumberCommandStrategy(InitiativeCountersService initiativeCountersService) {
        this.initiativeCountersService = initiativeCountersService;
    }

    @Override
    public String getCommandType() {
        return "DELETE_SEQUENCE_NUMBER";
    }

    @Override
    public void processCommand(QueueCommandOperationDTO queueCommandOperationDTO) {

        String initiativeId = queueCommandOperationDTO.getEntityId();
        Map<String,String> properties = queueCommandOperationDTO.getProperties();

        if (initiativeId == null ||
                properties == null ||
                !properties.containsKey(SEQUENCE_NUMBER_PROPERTY)
        ) {
            log.info("[DELETE_SEQUENCE_NUMBER_COMMAND] Command sent with " +
                    "missing mandatory parameters, discarding");
            return;
        }

        long sequenceNumber;
        try {
            sequenceNumber = Long.parseLong(properties.get(SEQUENCE_NUMBER_PROPERTY));
        } catch (Exception e) {
            log.info("[DELETE_SEQUENCE_NUMBER_COMMAND] Command sent with unreadable " +
                    "sequence number property, discarding");
            return;
        }

        initiativeCountersService.removeMessageToProcessOnInitative(initiativeId, sequenceNumber);

    }

}
