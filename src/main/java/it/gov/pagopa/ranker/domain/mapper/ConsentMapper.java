package it.gov.pagopa.ranker.domain.mapper;

import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.domain.model.Onboarding;
import org.springframework.stereotype.Service;

@Service
public class ConsentMapper {

  public OnboardingDTO map(Onboarding onboarding) {

    return OnboardingDTO.builder()
        .userId(onboarding.getUserId())
        .initiativeId(onboarding.getInitiativeId())
        .status(onboarding.getStatus())
        .pdndAccept(onboarding.getPdndAccept())
        .criteriaConsensusTimestamp(onboarding.getCriteriaConsensusTimestamp())
        .tc(onboarding.getTc())
        .tcAcceptTimestamp(onboarding.getTcAcceptTimestamp())
        .userMail(onboarding.getUserMail())
        .channel(onboarding.getChannel())
        .name(onboarding.getName())
        .surname(onboarding.getSurname())
        .build();

  }

}
