package it.gov.pagopa.ranker.controller;

import it.gov.pagopa.ranker.domain.dto.ManualDequeueDTO;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.domain.mapper.ManualDequeueMapper;
import it.gov.pagopa.ranker.exception.ManualDequeueSizeNotValidException;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import static it.gov.pagopa.ranker.constants.ErrorMessages.INVALID_SIZE_NOT_POSITIVE;

@RestController
@Slf4j
public class RankerControllerImpl implements RankerController {

    private final RankerService rankerService;
    private final ManualDequeueMapper manualDequeueMapper;

    public RankerControllerImpl(RankerService rankerService, ManualDequeueMapper manualDequeueMapper) {
        this.rankerService = rankerService;
        this.manualDequeueMapper = manualDequeueMapper;
    }

    @Override
    public void preallocate(OnboardingDTO onboardingDTO) {
        rankerService.preallocate(onboardingDTO);
    }

    @Override
    public void recovery(OnboardingDTO onboardingDTO) {
        rankerService.recovery(onboardingDTO);
    }

    @Override
    public ResponseEntity<ManualDequeueDTO> manualDequeue(String initiativeId, int size) {
        // Checking if size parameter is valid
        if (size < 1) {
            log.error("Error when calling manualDequeue | size is not positive, its value is: {}", size);
            throw new ManualDequeueSizeNotValidException(INVALID_SIZE_NOT_POSITIVE);
        }
        // TODO: Should we validate initiativeId value or domain as well?

        // TODO: call async orchestrator to handle new/current sessionworker logic
        // maybe ManualDequeueOrchestrator(+Impl) that then calls a SessionWorker/ManualSessionWorker?

        return ResponseEntity.accepted().body(manualDequeueMapper.toAccepted(initiativeId, size));
    }
}
