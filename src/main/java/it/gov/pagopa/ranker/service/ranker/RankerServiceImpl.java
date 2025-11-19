package it.gov.pagopa.ranker.service.ranker;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.ranker.connector.event.producer.RankerProducer;
import it.gov.pagopa.ranker.constants.OnboardingConstant;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.domain.mapper.ConsentMapper;
import it.gov.pagopa.ranker.domain.model.InitiativeCountersPreallocations;
import it.gov.pagopa.ranker.domain.model.Onboarding;
import it.gov.pagopa.ranker.repository.OnboardingRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.utils.CommonConstants.ZONEID;

@Service
@Slf4j
public class RankerServiceImpl implements RankerService {

    private final RankerProducer rankerProducer;
    private final InitiativeCountersService initiativeCountersService;
    private final OnboardingRepository onboardingRepository;
    private final ConsentMapper consentMapper;
    private final ObjectReader objectReader;
    private final List<String> initiativesId;

    public RankerServiceImpl(RankerProducer rankerProducer,
                             InitiativeCountersService initiativeCountersService,
                             OnboardingRepository onboardingRepository,
                             ConsentMapper consentMapper,
                             ObjectMapper objectMapper,
                             @Value("${app.initiative.identified}") List<String> initiativesId) {
        this.rankerProducer = rankerProducer;
        this.initiativeCountersService = initiativeCountersService;
        this.onboardingRepository = onboardingRepository;
        this.consentMapper = consentMapper;
        this.objectReader = objectMapper.readerFor(OnboardingDTO.class);
        this.initiativesId = initiativesId;
    }

    @Override
    public void preallocate(OnboardingDTO dto) {
        if (this.initiativeCountersService.existsByInitiativeIdAndUserId(dto.getInitiativeId(), dto.getUserId())) {
            log.info("User {} already preallocated for initiative {}", sanitizeString(dto.getUserId()), sanitizeString(dto.getInitiativeId()));
            return;
        }

        if(!this.initiativesId.contains(dto.getInitiativeId())){
            log.error("[RANKER_SERVICE] Skipped processing for initiativeId={} because it is not configured among handled initiatives", sanitizeString(dto.getInitiativeId()));
            return;
        }

        this.initiativeCountersService.addPreallocatedUser(
                dto.getInitiativeId(),
                dto.getUserId(),
                Boolean.TRUE.equals(dto.getVerifyIsee()),
                dto.getSequenceNumber(),
                dto.getEnqueuedTime()
        );

        log.info("Preallocation added for user {} in initiative {}", sanitizeString(dto.getUserId()), sanitizeString(dto.getInitiativeId()));
        this.rankerProducer.sendSaveConsent(dto);
    }

    @Override
    public void recovery(OnboardingDTO inputDto) {
        Optional<InitiativeCountersPreallocations> initiativeCountersPreallocations = this.initiativeCountersService.findById(inputDto.getInitiativeId(), inputDto.getUserId());
        Optional<Onboarding> onboarding = this.onboardingRepository.findById(Onboarding.buildId(inputDto.getInitiativeId(), inputDto.getUserId()));
        if (initiativeCountersPreallocations.isPresent() && onboarding.isPresent()
            && OnboardingConstant.ON_EVALUATION.equals(onboarding.get().getStatus())) {

            OnboardingDTO sendDto = consentMapper.map(onboarding.get());
            sendDto.setServiceId(inputDto.getServiceId());
            sendDto.setVerifyIsee(initiativeCountersPreallocations.get().getPreallocatedAmountCents() > 10000);

            log.info("[RANKER_SERVICE] Preallocation exists for userId={} and initiativeId={}. Resending save consent.",
                    sanitizeString(inputDto.getUserId()),
                    sanitizeString(inputDto.getInitiativeId())
            );
            this.rankerProducer.sendSaveConsent(sendDto);
        } else {
            log.error("[RANKER_SERVICE] Data inconsistency detected for userId={} and initiativeId={}: preallocation exists: {}, onboarding exists: {}, status check: {}",
                    sanitizeString(inputDto.getUserId()),
                    sanitizeString(inputDto.getInitiativeId()),
                    initiativeCountersPreallocations.isPresent(),
                    onboarding.isPresent(),
                    onboarding.isPresent() ? onboarding.get().getStatus() : "N/A"
            );
        }
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

    public static String sanitizeString(String str){
        return str == null? null: str.replaceAll("[\\r\\n]", "").replaceAll("[^\\w\\s-]", "");
    }
}
