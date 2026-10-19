package it.gov.pagopa.ranker.service.ranker;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import it.gov.pagopa.ranker.connector.event.producer.RankerProducer;
import it.gov.pagopa.ranker.constants.OnboardingConstant;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.domain.mapper.ConsentMapper;
import it.gov.pagopa.ranker.domain.model.InitiativeCountersPreallocations;
import it.gov.pagopa.ranker.domain.model.Onboarding;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import it.gov.pagopa.ranker.repository.OnboardingRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static it.gov.pagopa.utils.CommonConstants.ZONEID;
import static it.gov.pagopa.utils.CommonUtils.sanitizeString;

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
        if(!this.initiativesId.contains(dto.getInitiativeId())){
            log.error("[RANKER_SERVICE] Skipped processing for initiativeId={} because it is not configured among handled initiatives", sanitizeString(dto.getInitiativeId()));
            return;
        }

        Long attemptSequenceNumber = dto.getSequenceNumber();
        LocalDateTime attemptEnqueuedTime = getAttemptEnqueuedTime(dto);

        Optional<InitiativeCountersPreallocations> existingPreallocation =
                this.initiativeCountersService.findById(dto.getInitiativeId(), dto.getUserId());
        if (existingPreallocation.isPresent()) {
            InitiativeCountersPreallocations existing = existingPreallocation.get();
            PreallocationStatus status = existing.getStatus();
            if (List.of(PreallocationStatus.REFUNDED, PreallocationStatus.EXPIRED, PreallocationStatus.REJECTED).contains(status)) {
                if (isRedeliveredOrOlderAttempt(existing, attemptSequenceNumber, attemptEnqueuedTime)) {
                    log.info("[PREALLOCATE] Skipping redelivered or older terminal request for user {} and initiative {}",
                            sanitizeString(dto.getUserId()),
                            sanitizeString(dto.getInitiativeId()));
                    return;
                }
                boolean reopened = this.initiativeCountersService.reopenPreallocatedUser(
                        dto.getInitiativeId(),
                        dto.getUserId(),
                        dto.getVerifies(),
                        attemptSequenceNumber,
                        attemptEnqueuedTime
                );
                if (reopened) {
                    log.info("[PREALLOCATE] User {} has terminal status {} for initiative {}. Reopening preallocation and re-sending consent.",
                            sanitizeString(dto.getUserId()),
                            status,
                            sanitizeString(dto.getInitiativeId()));
                    sendConsentAndMark(dto, attemptSequenceNumber, attemptEnqueuedTime);
                } else {
                    log.warn("[PREALLOCATE] Cannot reopen preallocation for user {} and initiative {}.",
                            sanitizeString(dto.getUserId()),
                            sanitizeString(dto.getInitiativeId()));
                }
                return;
            }

            if (PreallocationStatus.PREALLOCATED.equals(status)
                    && !Boolean.TRUE.equals(existing.getConsentSent())
                    && isSameAttempt(existing, attemptSequenceNumber, attemptEnqueuedTime)) {
                log.info("[PREALLOCATE] Retrying pending consent send for user {} and initiative {}",
                        sanitizeString(dto.getUserId()),
                        sanitizeString(dto.getInitiativeId()));
                sendConsentAndMark(dto, attemptSequenceNumber, attemptEnqueuedTime);
                return;
            }

            log.info("[PREALLOCATE] User {} already preallocated for initiative {} with status {}",
                    sanitizeString(dto.getUserId()),
                    sanitizeString(dto.getInitiativeId()),
                    status);
            return;
        }

        this.initiativeCountersService.addPreallocatedUser(
                dto.getInitiativeId(),
                dto.getUserId(),
                dto.getVerifies(),
                attemptSequenceNumber,
                attemptEnqueuedTime

        );

        log.info("[PREALLOCATE] Preallocation added for user {} in initiative {}", sanitizeString(dto.getUserId()), sanitizeString(dto.getInitiativeId()));
        sendConsentAndMark(dto, attemptSequenceNumber, attemptEnqueuedTime);
    }

    @Override
    public void recovery(OnboardingDTO inputDto) {
        Optional<InitiativeCountersPreallocations> initiativeCountersPreallocations = this.initiativeCountersService.findById(inputDto.getInitiativeId(), inputDto.getUserId());
        Optional<Onboarding> onboarding = this.onboardingRepository.findById(Onboarding.buildId(inputDto.getInitiativeId(), inputDto.getUserId()));
        if (initiativeCountersPreallocations.isPresent() && onboarding.isPresent()
            && (OnboardingConstant.ON_EVALUATION.equals(onboarding.get().getStatus())
                    || OnboardingConstant.UNSUBSCRIBED.equals(onboarding.get().getStatus()))
        ) {

            OnboardingDTO sendDto = consentMapper.map(onboarding.get());
            sendDto.setServiceId(inputDto.getServiceId());

            //TODO: capire cosa fare qui
            //sendDto.setVerifyIsee(initiativeCountersPreallocations.get().getPreallocatedAmountCents() > 10000);

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
            return objectReader.readValue(messageBody);
        } catch (Exception e) {
            log.error("[RANKER_PROCESSOR] Failed to deserialize message");
            throw new IllegalStateException("Failed to deserialize message", e);
        }
    }

    private boolean isSameAttempt(InitiativeCountersPreallocations preallocation, Long sequenceNumber, LocalDateTime enqueuedTime) {
        return Objects.equals(preallocation.getSequenceNumber(), sequenceNumber)
                && Objects.equals(preallocation.getEnqueuedTime(), enqueuedTime);
    }

    private LocalDateTime getAttemptEnqueuedTime(OnboardingDTO dto) {
        return dto.getEnqueuedTime() != null ? dto.getEnqueuedTime() : dto.getTcAcceptTimestamp();
    }

    private boolean isRedeliveredOrOlderAttempt(InitiativeCountersPreallocations existing, Long sequenceNumber, LocalDateTime enqueuedTime) {
        if (existing.getSequenceNumber() != null && sequenceNumber != null) {
            return sequenceNumber <= existing.getSequenceNumber();
        }

        if (existing.getEnqueuedTime() != null && enqueuedTime != null) {
            return !enqueuedTime.isAfter(existing.getEnqueuedTime());
        }

        // If attempt identity is missing, avoid reserving budget again.
        return existing.getSequenceNumber() == null && existing.getEnqueuedTime() == null
                && sequenceNumber == null && enqueuedTime == null;
    }

    private void sendConsentAndMark(OnboardingDTO dto, Long sequenceNumber, LocalDateTime enqueuedTime) {
        this.rankerProducer.sendSaveConsent(dto);
        this.initiativeCountersService.markConsentSent(
                dto.getInitiativeId(),
                dto.getUserId(),
                sequenceNumber,
                enqueuedTime
        );
    }
}
