package it.gov.pagopa.ranker.strategy.command;

import it.gov.pagopa.ranker.domain.dto.QueueCommandOperationDTO;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

import static it.gov.pagopa.ranker.constants.CommonConstants.SEQUENCE_NUMBER_PROPERTY;


@Service
@Slf4j
public class AddSequenceNumberCommandStrategy implements CommandProcessorStrategy {

    private RankerService rankerService;

    @Override
    public String getCommandType() {
        return "ADD_SEQUENCE_NUMBER";
    }

    @Override
    public void processCommand(QueueCommandOperationDTO queueCommandOperationDTO) {

        String initiativeId = queueCommandOperationDTO.getEntityId();
        Map<String,String> properties = queueCommandOperationDTO.getProperties();

        if (initiativeId == null ||
            properties == null ||
            !properties.containsKey(SEQUENCE_NUMBER_PROPERTY)
        ) {
            log.info("[ADD_SEQUENCE_NUMBER_COMMAND] Command sent with missing " +
                    "mandatory parameters, discarding");
            return;
        }

        long sequenceNumber;
        try {
            sequenceNumber = Long.parseLong(properties.get(SEQUENCE_NUMBER_PROPERTY));
        } catch (Exception e) {
            log.info("[ADD_SEQUENCE_NUMBER_COMMAND] Command sent with unreadable " +
                    "sequence number property, discarding");
            return;
        }

        rankerService.addSequenceIdToInitiative(initiativeId, sequenceNumber);

    }

}
