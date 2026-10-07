package it.gov.pagopa.ranker.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualDequeueDTO {

    String initiativeId;
    Integer requestedMessages;
    String status;
    String traceId;
}
