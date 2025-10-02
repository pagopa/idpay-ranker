package it.gov.pagopa.ranker.service.ranker;

import it.gov.pagopa.ranker.connector.event.producer.RankerProducer;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.exception.ResourceNotReadyException;
import it.gov.pagopa.ranker.exception.MessageProcessingException;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RankerServiceImpl implements RankerService {

    private final RankerProducer rankerProducer;
    private final InitiativeCountersRepository initiativeCountersRepository;
    private final InitiativeCountersService initiativeCountersService;

    private boolean consumerActive = true;

    public RankerServiceImpl(RankerProducer rankerProducer,
                             InitiativeCountersRepository initiativeCountersRepository,
                             InitiativeCountersService initiativeCountersService) {
        this.rankerProducer = rankerProducer;
        this.initiativeCountersRepository = initiativeCountersRepository;
        this.initiativeCountersService = initiativeCountersService;
    }

    @Override
    public void execute(OnboardingDTO dto, Long sequenceNumber, Long enqueuedTime) {
        if (!consumerActive) {
            log.info("Consumer is inactive, skipping message for user {}", dto.getUserId());
            throw new MessageProcessingException("Consumer inactive, message not processed");
        }

        var optional = initiativeCountersRepository.findById(dto.getInitiativeId());
        if (optional.isEmpty()) {
            log.error("Counter or initiative {} aren't ready yet", dto.getInitiativeId());
            throw new ResourceNotReadyException();
        }

        if (initiativeCountersRepository.existsByInitiativeIdAndUserId(dto.getInitiativeId(), dto.getUserId())) {
            log.info("User {} already preallocated for initiative {}", dto.getUserId(), dto.getInitiativeId());
            return;
        }

        initiativeCountersService.addedPreallocatedUser(
                dto.getInitiativeId(),
                dto.getUserId(),
                Boolean.TRUE.equals(dto.getVerifyIsee()),
                sequenceNumber,
                enqueuedTime
        );

        log.info("Preallocation added for user {} in initiative {}", dto.getUserId(), dto.getInitiativeId());
        rankerProducer.sendSaveConsent(dto);

        var counters = initiativeCountersRepository.findById(dto.getInitiativeId())
                .orElseThrow(ResourceNotReadyException::new);

        if (counters.getResidualInitiativeBudgetCents() < 100) {
            log.info("Residual budget below threshold for initiative {}, stopping consumer", dto.getInitiativeId());
            stopConsumer();
        }

        // if (dto.getInitiativeEndDate() != null && LocalDate.now().isAfter(dto.getInitiativeEndDate().toLocalDate())) {
        //     log.info("Iniziativa {} terminata, rifiuto utente {}", dto.getInitiativeId(), dto.getUserId());
        //     rankerProducer.sendOnboardingKo(dto, REJECTION_REASON_INITIATIVE_ENDED);
        // }
    }


    public void startConsumer() {
        if (!consumerActive) {
            log.info("Starting Ranker consumer because residual budget >= 100");
            consumerActive = true;
        }
    }

    public void stopConsumer() {
        if (consumerActive) {
            log.info("Stopping Ranker consumer because residual budget < 100");
            consumerActive = false;
        }
    }
}
