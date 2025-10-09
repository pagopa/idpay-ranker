package it.gov.pagopa.ranker.service.ranker;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.ranker.connector.event.producer.RankerProducer;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static it.gov.pagopa.utils.CommonConstants.ZONEID;

@Service
@Slf4j
public class RankerServiceImpl implements RankerService {

    private final RankerProducer rankerProducer;
    private final InitiativeCountersRepository initiativeCountersRepository;
    private final InitiativeCountersService initiativeCountersService;
    private final ObjectReader objectReader;

    public RankerServiceImpl(RankerProducer rankerProducer,
                             InitiativeCountersRepository initiativeCountersRepository,
                             InitiativeCountersService initiativeCountersService,
                             ObjectMapper objectMapper) {
        this.rankerProducer = rankerProducer;
        this.initiativeCountersRepository = initiativeCountersRepository;
        this.initiativeCountersService = initiativeCountersService;
        this.objectReader = objectMapper.readerFor(OnboardingDTO.class);
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
    public void execute(ServiceBusReceivedMessage message) {
        OnboardingDTO onboarding = extractMessageHeader(message);
        this.preallocate(onboarding);

    }

    private OnboardingDTO extractMessageHeader(ServiceBusReceivedMessage message){
        OnboardingDTO onboardingDTO = deserialize(message.getBody().toString());

        long sequenceNumber = message.getSequenceNumber();
        LocalDateTime enqueuedTime = message.getEnqueuedTime().atZoneSameInstant(ZONEID).toLocalDateTime();

        onboardingDTO.setSequenceNumber(sequenceNumber);
        onboardingDTO.setEnqueuedTime(enqueuedTime);
        return onboardingDTO;
    }

    private OnboardingDTO deserialize(String messageBody) {
        try {
            return objectReader.readValue(messageBody, OnboardingDTO.class);
        } catch (Exception e) {
            log.error("[RANKER_PROCESSOR] Failed to deserialize message");
            throw new IllegalStateException("Failed to deserialize message", e);
        }
    }
}
