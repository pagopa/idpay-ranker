package it.gov.pagopa.ranker.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class VerifyDTO {

    private String code;
    private boolean verify;
    private String thersoldCode;
    private Long beneficiaryBudgetCentsMin;
    private Long beneficiaryBudgetCentsMax;
    private boolean blockingVerify;


}
