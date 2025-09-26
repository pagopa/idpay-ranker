package it.gov.pagopa.ranker.service.ranker;

import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;

public interface RankerService {
    void execute(OnboardingDTO dto);
}
