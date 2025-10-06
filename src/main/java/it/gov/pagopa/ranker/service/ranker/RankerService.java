package it.gov.pagopa.ranker.service.ranker;

import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import org.springframework.messaging.Message;

public interface RankerService {
    void execute(Message<OnboardingDTO> dto);
}
