package it.gov.pagopa.ranker.service.ranker.manualdequeue;

import it.gov.pagopa.ranker.domain.dto.ManualDequeueResponseDTO;

public interface ManualDequeueService {
    ManualDequeueResponseDTO requestDequeue(String initiativeId, int size);
}
