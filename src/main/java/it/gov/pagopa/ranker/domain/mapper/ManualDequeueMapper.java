package it.gov.pagopa.ranker.domain.mapper;

import it.gov.pagopa.ranker.domain.dto.ManualDequeueDTO;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class ManualDequeueMapper {

    private static final String MDC_TRACE_ID = "trace_id";
    private static final String STATUS_ACCEPTED = "ACCEPTED";

    public ManualDequeueDTO toAccepted(String initiativeId, int requestedMessages) {
        return ManualDequeueDTO.builder()
                .initiativeId(initiativeId)
                .requestedMessages(requestedMessages)
                .status(STATUS_ACCEPTED)
                .traceId(MDC.get(MDC_TRACE_ID))
                .build();
    }
}
