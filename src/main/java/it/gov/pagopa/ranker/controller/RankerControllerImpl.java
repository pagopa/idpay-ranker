package it.gov.pagopa.ranker.controller;

import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RankerControllerImpl implements RankerController {

    private final RankerService rankerService;

    public RankerControllerImpl(RankerService rankerService) {
        this.rankerService = rankerService;
    }

    @Override
    public void preallocate(OnboardingDTO onboardingDTO) {
        rankerService.preallocate(onboardingDTO);
    }
}
