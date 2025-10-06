package it.gov.pagopa.ranker.service.ranker;

import it.gov.pagopa.ranker.connector.event.producer.RankerProducer;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.exception.MessageProcessingException;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@Slf4j
public class RankerServiceImpl implements RankerService {

    private final RankerProducer rankerProducer;
    private final InitiativeCountersRepository initiativeCountersRepository;
    private final InitiativeCountersService initiativeCountersService;

    public RankerServiceImpl(RankerProducer rankerProducer,
                             InitiativeCountersRepository initiativeCountersRepository,
                             InitiativeCountersService initiativeCountersService) {
        this.rankerProducer = rankerProducer;
        this.initiativeCountersRepository = initiativeCountersRepository;
        this.initiativeCountersService = initiativeCountersService;
    }

    private void preallocate(OnboardingDTO dto) {
        if (initiativeCountersRepository.existsByInitiativeIdAndUserId(dto.getInitiativeId(), dto.getUserId())) {
            log.info("User {} already preallocated for initiative {}", dto.getUserId(), dto.getInitiativeId());
            return;
        }

        initiativeCountersService.addPreallocatedUser(
                dto.getInitiativeId(),
                dto.getUserId(),
                Boolean.TRUE.equals(dto.getVerifyIsee()),
                dto.getSequenceNumber(),
                dto.getEnqueuedTime()
        );

        log.info("Preallocation added for user {} in initiative {}", dto.getUserId(), dto.getInitiativeId());
        rankerProducer.sendSaveConsent(dto);
    }

    @Override
    public void execute(Message<OnboardingDTO> message) {
            try {
                OnboardingDTO dto = extractMessageHeader(message);
                this.preallocate(dto);
            } catch (MessageProcessingException e) {
                throw e;
            } catch (Exception e) {
                throw new MessageProcessingException("Error processing message", e);
            }
    }

    private OnboardingDTO extractMessageHeader(Message<OnboardingDTO> message){
        OnboardingDTO onboardingDTO = message.getPayload();

        Object seqObj = message.getHeaders().get("azure_service_bus_sequence_number");
        Object enqObj = message.getHeaders().get("azure_service_bus_enqueued_time");

        long sequenceNumber;
        ZoneId italyZone = ZoneId.of("Europe/Rome");
        LocalDateTime enqueuedTime;

        if (seqObj instanceof Number number) {
            sequenceNumber = number.longValue();
        } else {
            throw new MessageProcessingException("Missing sequenceNumber.");
        }

        if (enqObj instanceof Instant instant) {
            enqueuedTime = LocalDateTime.ofInstant(instant, italyZone);
        } else if (enqObj instanceof Number enqueuedNumber) {
            enqueuedTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(enqueuedNumber.longValue()), italyZone);
        } else if (enqObj instanceof String str) {
            enqueuedTime = LocalDateTime.parse(str);
        } else if (enqObj instanceof OffsetDateTime time) {
            enqueuedTime = time.atZoneSameInstant(italyZone).toLocalDateTime();
        } else {
            throw new MessageProcessingException("Missing enqueuedTime.");
        }

        onboardingDTO.setSequenceNumber(sequenceNumber);
        onboardingDTO.setEnqueuedTime(enqueuedTime);
        return onboardingDTO;
    }
}
