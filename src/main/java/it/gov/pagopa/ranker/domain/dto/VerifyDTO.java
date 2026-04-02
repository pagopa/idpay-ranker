package it.gov.pagopa.ranker.domain.dto;

import com.azure.resourcemanager.monitor.models.OnboardingStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class VerifyDTO {

    private String code;
    private boolean verify;
    private String thersoldCode;
    private Long beneficiaryBudgetCentsMin;
    private Long beneficiaryBudgetCentsMax;
    private boolean resultVerify;


}
