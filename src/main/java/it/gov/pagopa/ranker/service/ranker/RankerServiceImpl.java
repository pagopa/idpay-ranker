package it.gov.pagopa.ranker.service.ranker;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.ranker.connector.event.producer.CommandsProducer;
import it.gov.pagopa.ranker.connector.event.producer.RankerProducer;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.domain.dto.QueueCommandOperationDTO;
import it.gov.pagopa.ranker.exception.UnableToAddSeqException;
import it.gov.pagopa.ranker.exception.UnableToRemoveSeqException;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static it.gov.pagopa.ranker.constants.CommonConstants.*;

@Service
@Slf4j
public class RankerServiceImpl implements RankerService {

    private final RankerProducer rankerProducer;
    private final InitiativeCountersService initiativeCountersService;
    private final ObjectReader objectReader;
    private final List<String> initiativesId;
    private final CommandsProducer commandsProducer;

    public RankerServiceImpl(RankerProducer rankerProducer,
                             InitiativeCountersService initiativeCountersService,
                             ObjectMapper objectMapper,
                             @Value("${app.initiative.identified}") List<String> initiativesId, CommandsProducer commandsProducer) {
        this.rankerProducer = rankerProducer;
        this.initiativeCountersService = initiativeCountersService;
        this.objectReader = objectMapper.readerFor(OnboardingDTO.class);
        this.initiativesId = initiativesId;
        this.commandsProducer = commandsProducer;
    }

    private void preallocate(OnboardingDTO dto) {

        if (this.initiativeCountersService.existsByInitiativeIdAndUserId(dto.getInitiativeId(), dto.getUserId())) {
            log.info("User {} already preallocated for initiative {}", dto.getUserId(), dto.getInitiativeId());
            return;
        }

        if(!this.initiativesId.contains(dto.getInitiativeId())){
            log.error("[RANKER_SERVICE] Skipped processing for initiativeId={} because it is not " +
                    "configured among handled initiatives", dto.getInitiativeId());
            return;
        }

        this.initiativeCountersService.addPreallocatedUser(
                dto.getInitiativeId(),
                dto.getUserId(),
                Boolean.TRUE.equals(dto.getVerifyIsee()),
                dto.getSequenceNumber(),
                dto.getEnqueuedTime()
        );

        log.info("Preallocation added for user {} in initiative {}", dto.getUserId(), dto.getInitiativeId());
        this.rankerProducer.sendSaveConsent(dto);

    }

    @Override
    public void execute(ServiceBusReceivedMessage message) {
        OnboardingDTO onboarding = extractMessageHeader(message);
        this.preallocate(onboarding);

    }

    @Override
    public void addSequenceIdToInitiative(ServiceBusReceivedMessage message) {
        OnboardingDTO onboarding = extractMessageHeader(message);
        try {
            this.initiativeCountersService.addMessageProcessOnInitiative(
                    message.getSequenceNumber(), onboarding.getInitiativeId());
        } catch (Exception e) {
            QueueCommandOperationDTO addSeqNumberCommand = QueueCommandOperationDTO.builder()
                    .entityId(onboarding.getInitiativeId())
                    .operationType(DELETE_SEQUENCE_NUMBER)
                    .operationTime(LocalDateTime.now())
                    .properties(Map.of(SEQUENCE_NUMBER_PROPERTY, String.valueOf(message.getSequenceNumber())))
                    .build();
            if(!commandsProducer.sendCommand(addSeqNumberCommand)){
                log.error("[RANKER_CONTEXT] - Initiative: {}. Something went wrong while " +
                        "sending the message on Commands Queue", message.getSequenceNumber());
            }
            throw new UnableToAddSeqException("Failed to add the sequence number to initiative");
        }
    }

    @Override
    public void addSequenceIdToInitiative(String initiativeId, Long sequenceNumber) {
        try {
            this.initiativeCountersService.addMessageProcessOnInitiative(sequenceNumber, initiativeId);
        } catch (Exception e) {
            throw new UnableToAddSeqException("Failed to add the sequence number to initiative");
        }
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
