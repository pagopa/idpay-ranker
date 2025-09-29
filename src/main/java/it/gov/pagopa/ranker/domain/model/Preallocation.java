package it.gov.pagopa.ranker.domain.model;

import it.gov.pagopa.ranker.enums.PreallocationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Preallocation {

    private String userId;
    private PreallocationStatus status;
    private LocalDateTime createdAt;
}
