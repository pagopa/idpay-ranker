package it.gov.pagopa.ranker.connector.event.producer;


import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class RankerProducer {

  private final String binder;

  private final StreamBridge streamBridge;

  public RankerProducer(StreamBridge streamBridge,
                        @Value("${spring.cloud.stream.bindings.kafka-onboarding-outcome}") String binder) {
    this.streamBridge = streamBridge;
    this.binder = binder;
  }

  public void sendSaveConsent(OnboardingDTO onboardingDTO) {
    streamBridge.send("rankerProcessorOut-out-0", binder, onboardingDTO);
  }

}
