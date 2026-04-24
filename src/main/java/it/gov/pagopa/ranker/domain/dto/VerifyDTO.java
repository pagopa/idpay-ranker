package it.gov.pagopa.ranker.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class VerifyDTO {

    @JsonProperty("code")
    private String code;

    /** indica se la verifica va eseguita */
    @JsonProperty("verify")
    private boolean verify;

    /** indica se il fallimento blocca l’onboarding */
    @JsonProperty("blockingVerify")
    private boolean blockingVerify;

    /** codice soglia (es. BELET25), può essere null */
    @JsonProperty("thresholdCode")
    private String thresholdCode;

    @JsonProperty("beneficiaryBudgetCentsMin")
    private Long beneficiaryBudgetCentsMin;

    @JsonProperty("beneficiaryBudgetCentsMax")
    private Long beneficiaryBudgetCentsMax;

    /** null = non ancora eseguita */
    @JsonProperty("result")
    private Boolean result;

}