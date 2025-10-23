package it.gov.pagopa.ranker.service.ranker;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;

public interface RankerService {
    void execute(ServiceBusReceivedMessage message);

    void addSequenceIdToInitiative(ServiceBusReceivedMessage message);

    void addSequenceIdToInitiative(String initiativeId, Long sequenceNumber);


}
