package it.gov.pagopa.ranker.controller;

import it.gov.pagopa.ranker.domain.dto.ManualDequeueResponseDTO;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.exception.ManualDequeueSizeNotValidException;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import it.gov.pagopa.ranker.service.ranker.manualdequeue.ManualDequeueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import static it.gov.pagopa.ranker.constants.ErrorMessages.INVALID_SIZE_NOT_POSITIVE;

@RestController
@Slf4j
public class RankerControllerImpl implements RankerController {

    private final RankerService rankerService;
    private final ManualDequeueService manualDequeueService;

    public RankerControllerImpl(RankerService rankerService,
                                ManualDequeueService manualDequeueService) {
        this.rankerService = rankerService;
        this.manualDequeueService = manualDequeueService;
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
    public ResponseEntity<ManualDequeueResponseDTO> manualDequeue(String initiativeId, int size) {
        // Checking if size parameter is valid.
        if (size < 1) {
            log.error("Error when calling manualDequeue | size is not positive, its value is: {}", size);
            throw new ManualDequeueSizeNotValidException(INVALID_SIZE_NOT_POSITIVE);
        }

        // Create the command and start it
        ManualDequeueResponseDTO response = manualDequeueService.requestDequeue(initiativeId, size);

        // Return
        return ResponseEntity.accepted().body(response);
    }
}
