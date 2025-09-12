package it.gov.pagopa.ranker.connector.event.consumer;

import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.service.RankerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class RankerConsumer {

  @Bean
  public Consumer<OnboardingDTO> consumerOnboarding(RankerService rankerService) {
    return rankerService::checkBudget;
  }

}
