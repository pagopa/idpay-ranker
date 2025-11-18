package it.gov.pagopa.ranker.connector.event.producer;


import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RankerProducer {

  private final String binder;

  private final StreamBridge streamBridge;

  public RankerProducer(@Value("${spring.cloud.stream.bindings.rankerProducer-out-0.binder}") String binder,
                        StreamBridge streamBridge) {
      this.binder = binder;
      this.streamBridge = streamBridge;
  }

  public void sendSaveConsent(OnboardingDTO onboardingDTO) {
    streamBridge.send("rankerProducer-out-0", onboardingDTO);
    log.info("Sending message: {}", sanitizeLog(onboardingDTO));
  }


  private String sanitizeLog(Object obj) {
      if (obj == null) return "null";
      return obj
              .toString()
              .replaceAll("[\\n\\r\\t]", "_")     // evita log spoofing su più righe
              .replaceAll("[|;]", "_");           // optional per separatori o injection
  }
}
