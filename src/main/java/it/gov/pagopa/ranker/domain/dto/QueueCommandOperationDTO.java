package it.gov.pagopa.ranker.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class QueueCommandOperationDTO {
    private String operationType;
    private String entityId;
    private LocalDateTime operationTime;
    @Builder.Default
    private Map<String, String> properties = new HashMap<>();
}
