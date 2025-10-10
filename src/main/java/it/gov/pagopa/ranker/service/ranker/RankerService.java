package it.gov.pagopa.ranker.service.ranker;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;

public interface RankerService {
    void execute(ServiceBusReceivedMessage message);
}
