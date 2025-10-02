package it.gov.pagopa.ranker.connector.event.consumer;

import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.exception.MessageProcessingException;
import it.gov.pagopa.ranker.service.ranker.RankerServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
public class RankerConsumer {

  private final RankerServiceImpl rankerServiceImpl;

  public RankerConsumer(RankerServiceImpl rankerServiceImpl) {
    this.rankerServiceImpl = rankerServiceImpl;
  }

  @Bean
  public Consumer<Message<OnboardingDTO>> rankerProcessor() {
    return message -> {
      try {
        OnboardingDTO onboardingDTO = message.getPayload();

        long sequenceNumber = 0L;
        long enqueuedTime = 0L;

        Object seqObj = message.getHeaders().get("sequenceNumber");
        Object enqObj = message.getHeaders().get("enqueuedTime");

        if (seqObj instanceof Number number) {
          sequenceNumber = number.longValue();
        }
        if (enqObj instanceof Number number) {
          enqueuedTime = number.longValue();
        }

        rankerServiceImpl.execute(onboardingDTO, sequenceNumber, enqueuedTime);
      } catch (MessageProcessingException e) {
        throw e;
      } catch (Exception e) {
        throw new MessageProcessingException("Error processing message", e);
      }
    };
  }
}
