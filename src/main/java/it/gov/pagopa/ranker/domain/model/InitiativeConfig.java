package it.gov.pagopa.ranker.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class InitiativeConfig {

    private String initiativeId;
    private String initiativeName;
    private String organizationId;
    private String organizationName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long initiativeBudgetCents;
    private Long beneficiaryInitiativeBudgetCents;
    private Long beneficiaryInitiativeBudgetMaxCents;
    private boolean rankingInitiative;
    private String initiativeRewardType;
}
