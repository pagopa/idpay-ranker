package it.gov.pagopa.ranker.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class VerifyDTO {
    @JsonProperty("code")
    private String code;
    /** indica se la verifica va eseguita */
    @JsonProperty("verify")
    private boolean verify;
    /** codice soglia, può essere null */
    @JsonProperty("thresholdCode")
    private String thresholdCode;
    @JsonProperty("beneficiaryBudgetCentsMin")
    private Long beneficiaryBudgetCentsMin;
    @JsonProperty("beneficiaryBudgetCentsMax")
    private Long beneficiaryBudgetCentsMax;
    /** indica se il fallimento blocca l'onboarding */
    @JsonProperty("resultVerify")
    private boolean blockingVerify;
}