package it.gov.pagopa.ranker.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank
    private String operationType;
    @NotBlank
    private String entityId;
    private LocalDateTime operationTime;
    @Builder.Default
    private Map<String, String> properties = new HashMap<>();
}
