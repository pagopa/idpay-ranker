package it.gov.pagopa.ranker.service.ranker;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;

public interface RankerService {
    void execute(ServiceBusReceivedMessage message);
    void preallocate(OnboardingDTO dto);
    void recovery(OnboardingDTO dto);
}
