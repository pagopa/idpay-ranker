package it.gov.pagopa.ranker.service.initative;

import it.gov.pagopa.ranker.domain.model.InitiativeConfig;

public interface InitiativeBeneficiaryRuleService {
    InitiativeConfig getInitiativeConfig(String initiativeId);
}
