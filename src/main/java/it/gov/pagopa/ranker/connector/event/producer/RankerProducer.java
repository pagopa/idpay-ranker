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
    log.info("Sending message: {}", getSafeLogString(onboardingDTO));
  }

/**
 * Builds a log-safe string from OnboardingDTO, sanitizing every (potentially user-provided) string field.
 */
    private String getSafeLogString(OnboardingDTO dto) {
        if (dto == null) return "null";
        // Sanitize each potentially user-controlled field. List all fields for clarity.
        return String.format(
                "OnboardingDTO(userId=%s, initiativeId=%s, name=%s, surname=%s, tc=%s, verifyIsee=%s, status=%s, pdndAccept=%s, userMail=%s, channel=%s, tcAcceptTimestamp=%s, criteriaConsensusTimestamp=%s, serviceId=%s, rejectionReason=%s, sequenceNumber=%s, enqueuedTime=%s)",
                sanitizeField(dto.getUserId()),
                sanitizeField(dto.getInitiativeId()),
                sanitizeField(dto.getName()),
                sanitizeField(dto.getSurname()),
                String.valueOf(dto.getTc()),
                String.valueOf(dto.getVerifyIsee()),
                sanitizeField(dto.getStatus()),
                String.valueOf(dto.getPdndAccept()),
                sanitizeField(dto.getUserMail()),
                sanitizeField(dto.getChannel()),
                String.valueOf(dto.getTcAcceptTimestamp()),
                String.valueOf(dto.getCriteriaConsensusTimestamp()),
                sanitizeField(dto.getServiceId()),
                sanitizeField(dto.getRejectionReason()),
                String.valueOf(dto.getSequenceNumber()),
                String.valueOf(dto.getEnqueuedTime())
        );
    }

    /**
     * Sanitizes a single string for logging: removes line breaks, carriage returns and non-word characters except whitespace and dash.
     */
    private static String sanitizeField(String str) {
        // Reuse the stricter sanitation logic used elsewhere in the project
        return str == null ? null : str.replaceAll("[\\r\\n]", "").replaceAll("[^\\w\\s-]", "");
    }
}
