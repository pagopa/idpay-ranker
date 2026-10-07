package it.gov.pagopa.ranker.controller;

import it.gov.pagopa.ranker.domain.dto.ManualDequeueDTO;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("idpay-itn/ranker")
public interface RankerController {

    @PostMapping("/preallocate")
    @ResponseStatus(code = HttpStatus.OK)
    public void preallocate(@RequestBody OnboardingDTO onboardingDTO);

    @PostMapping("/recovery")
    @ResponseStatus(code = HttpStatus.OK)
    public void recovery(@RequestBody OnboardingDTO onboardingDTO);

    @PostMapping("/{initiativeId}/dequeue")
    public ResponseEntity<ManualDequeueDTO> manualDequeue(
            @PathVariable("initiativeId") String initiativeId,
            @RequestParam(required = false, defaultValue = "1") int size
    );
}
